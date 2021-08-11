package pmap

import (
	"github.com/SpaiR/strongdmm/app/command"
	"github.com/SpaiR/strongdmm/pkg/util"
)

// AddSelectedInstance adds currently selected instance on the map.
// If there is no selected instance, then nothing will happen.
func (p *PaneMap) AddSelectedInstance(pos util.Point) {
	if instance := p.action.SelectedInstance(); instance != nil {
		tile := p.Dmm.GetTile(pos.X, pos.Y, 1) // TODO: respect Z-level
		tile.Content = append(tile.Content, instance)
		p.canvas.Render.UpdateBucketV(p.Dmm, []util.Point{pos})
	}
}

func (p *PaneMap) HasSelectedInstance() bool {
	return p.action.HasSelectedInstance()
}

// CommitChanges triggers snapshot to commit changes and create a patch between two map states.
func (p *PaneMap) CommitChanges(changesType string) {
	stateId := p.Snapshot.Commit()
	p.action.PushCommand(command.New(changesType, func() {
		p.Snapshot.GoTo(stateId - 1)
		p.canvas.Render.UpdateBucket(p.Dmm)
	}, func() {
		p.Snapshot.GoTo(stateId)
		p.canvas.Render.UpdateBucket(p.Dmm)
	}))
}
