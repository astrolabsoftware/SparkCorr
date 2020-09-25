import vtk
from numpy import *

a=1./sqrt(3)
#1 cube ###############
cubeSource=vtk.vtkCubeSource()
cubeSource.SetXLength(2*a)
cubeSource.SetYLength(2*a)
cubeSource.SetZLength(2*a)

#mapper
cubeMapper=vtk.vtkPolyDataMapper()
cubeMapper.SetInputConnection(cubeSource.GetOutputPort())
#actor
cubeActor = vtk.vtkActor()
cubeActor.SetMapper(cubeMapper)

cubeActor.GetProperty().SetColor(0.5, 0.5,0.5)
cubeActor.GetProperty().SetRepresentationToWireframe()
cubeActor.GetProperty().SetLineWidth(3)

#2 sphere ################
sphereSource=vtk.vtkSphereSource()
sphereSource.SetRadius(0.995)
sphereSource.SetThetaResolution(30)
sphereSource.SetPhiResolution(30)

sphereMapper=vtk.vtkPolyDataMapper()
sphereMapper.SetInputConnection(sphereSource.GetOutputPort())

#actor
sphereActor = vtk.vtkActor()
sphereActor.SetMapper(sphereMapper)

#sphereActor.GetProperty().SetColor(0.8,0.5,0.5)
sphereActor.GetProperty().SetColor(0.9,0.9,0.9)
sphereActor.GetProperty().SetOpacity(0.9)
sphereActor.GetProperty().SetRepresentationToWireframe()


##3 tiling#############
tiles=vtk.vtkAppendPolyData()

N=10

#equal angles
la=linspace(-pi/4,pi/4,N)
alp,bet=meshgrid(la,la)
x= a*tan(alp)
y= a*tan(bet)

#cobe
lx=linspace(-1.,1,N)
X,Y=meshgrid(lx,lx)

#mapping
#fortran indexing add 0
P=[0,-0.27292696,-0.07629969,-0.02819452,-0.22797056,-0.01471565,0.27058160,0.54852384,0.48051509,
       -0.56800938,-0.60441560,-0.62930065,-1.74114454,0.30803317,1.50880086,0.93412077,0.25795794,
       1.71547508,0.98938102,-0.93678576,-1.41601920,-0.63915306,0.02584375,-0.53022337,-0.83180469,
       0.08693841,0.33887446,0.52032238,0.14381585]

XX=X*X
YY=Y*Y

XI=X*(1.+(1.-XX)*(
   P[1]+XX*(P[2]+XX*(P[4]+XX*(P[7]+XX*(P[11]+XX*(P[16]+XX*P[22]))))) +
   YY*( P[3]+XX*(P[5]+XX*(P[8]+XX*(P[12]+XX*(P[17]+XX*P[23])))) +
   YY*( P[6]+XX*(P[9]+XX*(P[13]+XX*(P[18]+XX*P[24]))) +
   YY*( P[10]+XX*(P[14]+XX*(P[19]+XX*P[25])) + 
   YY*( P[15]+XX*(P[20]+XX*P[26]) +
   YY*( P[21]+XX*P[27] + YY*P[28])))) )))

ETA=Y*(1.+(1.-YY)*(
    P[1]+YY*(P[2]+YY*(P[4]+YY*(P[7]+YY*(P[11]+YY*(P[16]+YY*P[22]))))) +
    XX*( P[3]+YY*(P[5]+YY*(P[8]+YY*(P[12]+YY*(P[17]+YY*P[23])))) +
    XX*( P[6]+YY*(P[9]+YY*(P[13]+YY*(P[18]+YY*P[24]))) +
    XX*( P[10]+YY*(P[14]+YY*(P[19]+YY*P[25])) +
    XX*( P[15]+YY*(P[20]+YY*P[26]) +
    XX*( P[21]+YY*P[27] + XX*P[28])))) )))

x,y=a*XI,a*ETA

doNorm=True

normalize=lambda p: p/sqrt(p.dot(p))

projector={ 0:lambda x,y:[a,x,y],
            2:lambda x,y:[-a,-x,y],
            1:lambda x,y:[-x,a,y],
            3:lambda x,y:[x,-a,y],
            4:lambda x,y:[y,x,a],
            5:lambda x,y:[y,x,-a]
          }

def pix2coord(ipix):
    f=ipix%10
    ij=ipix//10
    i=ij//N
    j=ij%N
    return [f,i,j]

def coord2pix(f,i,j):
    return (i*N+j)*10+f

#nodes
pts=vtk.vtkPoints()
faces=[4]
for iface in faces:
    print("face=",iface)
    proj=projector[iface]
    #project faces

    for i in range(N):
        for j in range(N):
            ipix=coord2pix(iface,i,j)
            #print("pix",(iface,i,j),ipix)
            p=array(proj(x[i,j],y[i,j]))
            if doNorm:
                p=normalize(p)
            pts.InsertPoint(ipix,p)

#cells
polys=vtk.vtkCellArray()
colors = vtk.vtkFloatArray()


for iface in faces:
    for i in range(N-1):
        for j in range(N-1):
            cid=polys.InsertNextCell(4)

            ip=coord2pix(iface,i,j)
            polys.InsertCellPoint(ip)

            ip1=coord2pix(iface,i+1,j)
            polys.InsertCellPoint(ip1)

            ip2=coord2pix(iface,i+1,j+1)
            polys.InsertCellPoint(ip2)

            ip3=coord2pix(iface,i,j+1)
            polys.InsertCellPoint(ip3)
        
            print("cell",ip,ip1,ip2,ip3)

            val=random.uniform()
            colors.InsertTuple1(cid,val)
    
#complet
tiles=vtk.vtkPolyData()
tiles.SetPoints(pts)
tiles.SetPolys(polys)
#tiles.GetCellData().SetScalars(colors)
#tiles.SetLines(polys)


#mapper
tilesMapper=vtk.vtkPolyDataMapper()
tilesMapper.SetInputData(tiles)


#actor
tilesActor = vtk.vtkActor()
tilesActor.SetMapper(tilesMapper)
tilesActor.GetProperty().SetColor(0, 0.,0)
tilesActor.GetProperty().SetRepresentationToWireframe()
tilesActor.GetProperty().SetLineWidth(2)
#tilesActor.GetProperty().SetOpacity(0.8)

# 4 centers ###############
centers= vtk.vtkCellCenters()
#centers.SetInputConnection(tiles.GetOutputPort())
centers.SetInputData(tiles)

pts = vtk.vtkPointSource()
pts.SetNumberOfPoints(1)

glyph = vtk.vtkGlyph3D()
glyph.SetInputConnection(centers.GetOutputPort())
glyph.SetSourceConnection(pts.GetOutputPort())
glyph.OrientOff()
glyph.SetVectorModeToUseVector()
glyph.SetScaleModeToScaleByVector()
glyph.SetScaleFactor(0.01)

spikeMapper = vtk.vtkPolyDataMapper()
spikeMapper.SetInputConnection(glyph.GetOutputPort())

spikeActor = vtk.vtkActor()
spikeActor.SetMapper(spikeMapper)
spikeActor.GetProperty().SetColor(0.0, 0,0)
spikeActor.GetProperty().SetPointSize(3)


#renderer  ###############
ren1 = vtk.vtkRenderer()

#actors
ren1.AddActor(cubeActor)
#ren1.AddActor(sphereActor)
ren1.AddActor(tilesActor)
ren1.AddActor(spikeActor)

ren1.SetBackground(1,1,1)

#cam=ren1.GetActiveCamera()
#cam.Zoom(1.2)
#ren1.SetActiveCamera(cam)
#ren1.ResetCamera()


#window
renWin = vtk.vtkRenderWindow()
renWin.AddRenderer(ren1)
renWin.SetSize(900, 900)


#interactor
iren = vtk.vtkRenderWindowInteractor()
iren.SetRenderWindow(renWin)
style = vtk.vtkInteractorStyleTrackballCamera()
iren.SetInteractorStyle(style)

#run
iren.Initialize()
iren.Start()

