package util

import "github.com/SpaiR/imgui-go"

func ImGuiSetNextWindowCentered(size imgui.Vec2, condition imgui.Condition) {
	vp := imgui.GetMainViewport()
	center := vp.GetCenter()
	imgui.SetNextWindowPosV(imgui.Vec2{X: center.X - size.X/2, Y: center.Y - size.Y/2}, condition, imgui.Vec2{})
	imgui.SetNextWindowSizeV(size, condition)
}
