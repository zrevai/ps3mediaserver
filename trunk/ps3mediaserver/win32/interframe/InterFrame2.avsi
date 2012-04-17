#------------------------------------------------------------------------------#
#                                                                              #
#                        InterFrame 2.0 RC1 by SubJunk                         #
#                                                                              #
#         A frame interpolation script that makes accurate estimations         #
#                   about the content of non-existent frames                   #
#      Its main use is to give videos higher framerates like newer TVs do      #
#------------------------------------------------------------------------------#


# For instructions and further information see the included InterFrame.html
# For news go to spirton.com


function InterFrame(clip Input, string "Preset", string "Tuning", int "NewNum", int "NewDen", bool "GPU", string "InputType", int "OverrideAlgo", int "OverrideArea", int "Cores") {
	# Defaults
	Preset        = default(Preset      , "Medium")
	NewNum        = default(NewNum      , 0       )
	NewDen        = default(NewDen      , 0       )
	Tuning        = default(Tuning      , "Film"  )
	GPU           = default(GPU         , false   )
	InputType     = default(InputType   , "2D"    )
	OverrideAlgo  = default(OverrideAlgo, 0       )
	OverrideArea  = default(OverrideArea, 0       )

	# Convert integers to strings
	NewNum       = String(NewNum)
	NewDen       = String(NewDen)
	OverrideAlgo = String(OverrideAlgo)
	OverrideArea = String(OverrideArea)

	# Validate inputs
	Assert(Preset == "Medium" || Preset == "Fast", "'"+Preset+"' is not a valid preset. Please check the documentation for a list of the valid presets.")
	Assert(Tuning == "Film" || Tuning == "Smooth" || Tuning == "Animation" || Tuning == "Weak", "'"+Tuning+"' is not a valid tuning. Please check the documentation for a list of the valid tunings.")
	Assert(InputType == "2D" || InputType == "SBS" || InputType == "OU" || InputType == "HSBS" || InputType == "HOU", "'"+InputType+"' is not a valid InputType. Please check the documentation for a list of the valid InputTypes.")
	Assert(Defined(Cores), "You must enter a value for Cores. Please check the documentation for details.")

	# Get either 1 or 2 clips depending on InputType
	InputType == "SBS" ? Eval("""
		FirstEye  = InterFrameProcess(Input.crop(0            , 0, -Input.Width/2, 0), Preset=Preset, Tuning=Tuning, NewNum=NewNum, NewDen=NewDen, GPU=GPU, InputType=InputType, OverrideAlgo=OverrideAlgo, OverrideArea=OverrideArea, Cores=Cores)
		SecondEye = InterFrameProcess(Input.crop(Input.Width/2, 0, 0             , 0), Preset=Preset, Tuning=Tuning, NewNum=NewNum, NewDen=NewDen, GPU=GPU, InputType=InputType, OverrideAlgo=OverrideAlgo, OverrideArea=OverrideArea, Cores=Cores)
		StackHorizontal(FirstEye, SecondEye)
	""") : InputType == "OU" ? Eval("""
		FirstEye  = InterFrameProcess(Input.crop(0, 0             , 0, -Input.Height/2), Preset=Preset, Tuning=Tuning, NewNum=NewNum, NewDen=NewDen, GPU=GPU, InputType=InputType, OverrideAlgo=OverrideAlgo, OverrideArea=OverrideArea, Cores=Cores)
		SecondEye = InterFrameProcess(Input.crop(0, Input.Height/2, 0, 0              ), Preset=Preset, Tuning=Tuning, NewNum=NewNum, NewDen=NewDen, GPU=GPU, InputType=InputType, OverrideAlgo=OverrideAlgo, OverrideArea=OverrideArea, Cores=Cores)
		StackVertical(FirstEye, SecondEye)
	""") : InputType == "HSBS" ? Eval("""
		FirstEye  = InterFrameProcess(Input.crop(0            , 0, -Input.Width/2, 0).Spline36Resize(Input.Width, Input.Height), Preset=Preset, Tuning=Tuning, NewNum=NewNum, NewDen=NewDen, GPU=GPU, InputType=InputType, OverrideAlgo=OverrideAlgo, OverrideArea=OverrideArea, Cores=Cores)
		SecondEye = InterFrameProcess(Input.crop(Input.Width/2, 0, 0             , 0).Spline36Resize(Input.Width, Input.Height), Preset=Preset, Tuning=Tuning, NewNum=NewNum, NewDen=NewDen, GPU=GPU, InputType=InputType, OverrideAlgo=OverrideAlgo, OverrideArea=OverrideArea, Cores=Cores)
		StackHorizontal(FirstEye.Spline36Resize(Input.Width/2, Input.Height), SecondEye.Spline36Resize(Input.Width/2, Input.Height))
	""") : InputType == "HOU" ? Eval("""
		FirstEye  = InterFrameProcess(Input.crop(0, 0             , 0, -Input.Height/2).Spline36Resize(Input.Width, Input.Height), Preset=Preset, Tuning=Tuning, NewNum=NewNum, NewDen=NewDen, GPU=GPU, InputType=InputType, OverrideAlgo=OverrideAlgo, OverrideArea=OverrideArea, Cores=Cores)
		SecondEye = InterFrameProcess(Input.crop(0, Input.Height/2, 0, 0              ).Spline36Resize(Input.Width, Input.Height), Preset=Preset, Tuning=Tuning, NewNum=NewNum, NewDen=NewDen, GPU=GPU, InputType=InputType, OverrideAlgo=OverrideAlgo, OverrideArea=OverrideArea, Cores=Cores)
		StackVertical(FirstEye.Spline36Resize(Input.Width, Input.Height/2), SecondEye.Spline36Resize(Input.Width, Input.Height/2))
	""") : Eval("""
		InterFrameProcess(Input, Preset=Preset, Tuning=Tuning, NewNum=NewNum, NewDen=NewDen, GPU=GPU, InputType=InputType, OverrideAlgo=OverrideAlgo, OverrideArea=OverrideArea, Cores=Cores)
	""")
}

function InterFrameProcess(clip Input, string "Preset", string "Tuning", string "NewNum", string "NewDen", bool "GPU", string "InputType", string "OverrideAlgo", string "OverrideArea", int "Cores") {
	# Create SuperString
	GPU == true ? Eval("""
		SuperString = "{scale:{up:0,down:4},gpu:1,rc:false}"
	""") : Eval("""
		SuperString = "{scale:{up:2,down:4},gpu:0,rc:false}"
	""")

	# Create VectorsString
	VectorsString = (Tuning == "Weak") ? "{block:{w:32," : "{block:{w:16,"

	VectorsString = (Tuning == "Animation") ? VectorsString + "overlap:0" : VectorsString + "overlap:2"

	VectorsString = (Preset == "Fast") ? VectorsString + "},main:{search:{coarse:{distance:-10},bad:{sad:2000}}}}" : VectorsString + "},main:{search:{coarse:{distance:-10},bad:{sad:2000}}},refine:[{thsad:250}]}"

	# Create SmoothString
	NewNum != "0" ? Eval("""
		SmoothString = "{rate:{num:"+NewNum+",den:"+NewDen+",abs:true},"
	""") : Input.Framerate == 15 || Input.Framerate == 25 || Input.Framerate == 30 ? Eval("""
		SmoothString = "{rate:{num:2,den:1,abs:false},"
	""") : Eval("""
		SmoothString = "{rate:{num:60000,den:1001,abs:true},"
	""")

	OverrideAlgo != "0" ? Eval("""
		SmoothString = SmoothString + "algo:"+OverrideAlgo+",mask:{cover:80,"
	""") : Eval("""
		SmoothString = (Tuning == "Smooth") ? SmoothString + "algo:23,mask:{" : SmoothString + "algo:13,mask:{cover:80,"
	""")

	OverrideArea != "0" ? Eval("""
		SmoothString = SmoothString + "area:"+OverrideArea
	""") : Eval("""
		SmoothString = (Tuning == "Smooth" || Tuning == "Animation") ? SmoothString + "area:150" : SmoothString + "area:0"
	""")

	SmoothString = SmoothString + ",area_sharp:1.2},scene:{blend:true, mode:0}}"

	# Make interpolation vector clip
	Super   = SVSuper(Input, SuperString)
	Vectors = SVAnalyse(Super, VectorsString)

	# Put it together
	smooth_video = SVSmoothFps(Input, Super, Vectors, SmoothString, url="www.svp-team.com", mt=Cores)
	smooth_video
}
