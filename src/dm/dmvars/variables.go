package dmvars

import (
	"strconv"

	"sdmm/util/slice"
)

const NullValue = "null"

// Variables is a structure to store an instance (in the environment or on the map) variables.
// Those variables are stored as a "string2string" map and immutable by concept.
// It means that if you need to modify variables of an instance,
// then you need to create a new instance with modified variables.
// That restriction goes for the idea that instances by their nature are immutable as well.
type Variables struct {
	names  []string
	vars   map[string]string
	parent *Variables
}

func FromParent(parent *Variables) *Variables {
	return &Variables{parent: parent}
}

// MutableVariables are used to provide a basic modification interface,
// without breaking of an immutability of Variables struct.
type MutableVariables struct {
	Variables
}

func (v *MutableVariables) Put(name string, value string) {
	if v.vars == nil {
		v.vars = make(map[string]string)
	}
	if !slice.StrContains(v.names, name) {
		v.names = append(v.names, name)
	}
	v.vars[name] = value
}

func (v *MutableVariables) ToImmutable() *Variables {
	return &Variables{
		names:  v.names,
		vars:   v.vars,
		parent: v.parent,
	}
}

func (v *Variables) HasParent() bool {
	return v.parent != nil
}

func (v *Variables) LinkParent(parent *Variables) {
	if v.parent != nil {
		panic("Linking a parent to an occupied variables is prohibited!") // Just to ensure
	}
	v.parent = parent
}

func (v *Variables) Iterate() []string {
	return v.names
}

func (v *Variables) Len() int {
	return len(v.names)
}

func (v *Variables) Value(name string) (string, bool) {
	if v.vars != nil {
		if value, ok := v.vars[name]; ok {
			return value, true
		}
	}
	if v.parent != nil {
		return v.parent.Value(name)
	}
	return "", false
}

func (v *Variables) Text(name string) (string, bool) {
	if value, ok := v.Value(name); ok && value != NullValue {
		if len(value) > 1 {
			return value[1 : len(value)-1], true
		} else {
			return value, true
		}
	}
	return "", false
}

func (v *Variables) Float(name string) (float32, bool) {
	if value, ok := v.Value(name); ok && value != NullValue {
		if n, err := strconv.ParseFloat(value, 32); err == nil {
			return float32(n), true
		}
	}
	return 0, false
}

func (v *Variables) Int(name string) (int, bool) {
	if value, ok := v.Value(name); ok && value != NullValue {
		if n, err := strconv.ParseInt(value, 10, 32); err == nil {
			return int(n), true
		}
	}
	return 0, false
}
