package dialog

import (
	"github.com/SpaiR/imgui-go"
	"github.com/rs/zerolog/log"
)

type Type interface {
	Name() string
	Process()
	HasCloseButton() bool
}

const popupFlags = imgui.WindowFlagsAlwaysAutoResize | imgui.WindowFlagsNoSavedSettings

var opened []Type

func Process() {
	var closedDialogs []Type
	for _, dialog := range opened {
		if !imgui.IsPopupOpen(dialog.Name()) {
			imgui.OpenPopup(dialog.Name())
		}

		var isOpen bool
		if dialog.HasCloseButton() {
			open := true
			isOpen = imgui.BeginPopupModalV(dialog.Name(), &open, popupFlags)
		} else {
			isOpen = imgui.BeginPopupModalV(dialog.Name(), nil, popupFlags)
		}

		if isOpen {
			dialog.Process()
			imgui.EndPopup()
		}

		if !imgui.IsPopupOpen(dialog.Name()) {
			closedDialogs = append(closedDialogs, dialog)
		}
	}

	for _, dialog := range closedDialogs {
		Close(dialog)
	}
}

// Open opens the application dialog.
func Open(t Type) {
	log.Print("opening dialog:", t.Name())
	opened = append(opened, t)
}

// Close closed the application dialog.
func Close(dialog Type) {
	log.Print("closing dialog:", dialog.Name())
	for idx, t := range opened {
		if dialog.Name() == t.Name() {
			log.Print("dialog closed:", dialog.Name())
			opened = append(opened[:idx], opened[idx+1:]...)
			return
		}
	}
}
