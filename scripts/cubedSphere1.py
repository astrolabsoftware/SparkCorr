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
sphereActor.GetProperty().SetOpacity(0.1)
#sphereActor.GetProperty().SetRepresentationToWireframe()


##3 tiling#############
tiles=vtk.vtkAppendPolyData()

N=10
#north face
#xx=linspace(-a,a,N)
#yy=xx
#x,y=meshgrid(xx,yy)
#z=a

#equal angles
la=linspace(-pi/4,pi/4,N)

#la=arcsin(linspace(-1/sqrt(2),1/sqrt(2),N))

lb=la
alp,bet=meshgrid(la,la)
x= a*tan(alp)
y= a*tan(bet)

#xx=linspace(-1,1,N)
#yy=a*xx**2
#w=xx<0
#yy[w]=-yy[w]
#x,y=meshgrid(yy,yy)

doNorm=False

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
ren1.AddActor(sphereActor)
ren1.AddActor(tilesActor)
#ren1.AddActor(spikeActor)

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

