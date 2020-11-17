import vtk
from numpy import *
import sys

assert len(sys.argv)==2
fn=sys.argv[1]

#read file
print("reading "+fn)

p=loadtxt(fn)

nfaces=1
#number of nodes per face
N=int(sqrt(p.shape[0]/nfaces))
print("{} faces #nodes={}x{}".format(nfaces,N,N))

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

sphereActor.GetProperty().SetColor(0.9,0.9,0.9)
#sphereActor.GetProperty().SetOpacity(0.1)
#sphereActor.GetProperty().SetRepresentationToWireframe()


##3 tiling#############
tiles=vtk.vtkAppendPolyData()

pts=vtk.vtkPoints()

for ipix in range(len(p)):
    pts.InsertPoint(ipix,p[ipix])

#cells
polys=vtk.vtkCellArray()
colors = vtk.vtkFloatArray()

for iface in range(nfaces):
    for i in range(N-1):
        for j in range(N-1):
            ip=iface*N**2+N*i+j
            print("cen cell face={} i={} j={} ip={}".format(iface,i,j,ip))
            cid=polys.InsertNextCell(4)
            polys.InsertCellPoint(ip)
            polys.InsertCellPoint(ip+1)
            polys.InsertCellPoint(ip+N+1)
            polys.InsertCellPoint(ip+N)
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

