package main

import (
	"encoding/json"
	"fmt"
)

type InputCommand struct {
	PlayerId string `json:"playerId"`
	StartX   int64  `json:"startX"`
	StartY   int64  `json:"startY"`
	EndX     int64  `json:"endX"`
	EndY     int64  `json:"endY"`
}

func GetInputCommandFromJSON(inData []byte) (outCommand *InputCommand) {
	outCommand = &InputCommand{}
	err := json.Unmarshal(inData, outCommand)
	if err != nil {
		fmt.Println("Failed to parse input command. Error: ", err.Error())
	}
	return outCommand
}
