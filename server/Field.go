package main

import (
	"bytes"
	"fmt"
	"sync"
)

type GameField struct {
	fieldMutex *sync.RWMutex

	Width  int64
	Height int64
	stroke int64

	Rows []*GameFieldRow
}

func CreateGameField(inWidth int64, inHeight int64, inStroke int64) (outField *GameField) {
	outField = &GameField{}
	outField.fieldMutex = &sync.RWMutex{}
	outField.Width = inWidth
	outField.Height = inHeight
	outField.stroke = inStroke
	outField.Rows = make([]*GameFieldRow, inHeight)
	var i int64
	for i = 0; i < inHeight; i++ {
		outField.Rows[i] = CreateGameFieldRow(inWidth, inStroke, i)
	}
	return outField
}

func (inField *GameField) GetJSON() (outData []byte) {
	inField.fieldMutex.RLock()
	//outData, _ = json.Marshal(inField)
	outData = inField.generateJSON()
	inField.fieldMutex.RUnlock()
	return outData
}

func (inField *GameField) generateJSON() (outData []byte) {
	var buffer *bytes.Buffer = &bytes.Buffer{}
	buffer.WriteString(fmt.Sprintf("{\"width\":%d,\"height\":%d,\"data\":[", inField.Width, inField.Height))

	var i int64
	var lastIndex int64
	for i = 0; i < inField.Height; i++ {
		if inField.Rows[i].hasData {
			lastIndex = i
		}
	}

	for i = 0; i <= lastIndex; i++ {
		inField.Rows[i].GenerateOutput(buffer, i == lastIndex)
	}
	buffer.WriteString("]}")
	return buffer.Bytes()
}

func (inField *GameField) AddData(inData *InputCommand) (outChange []byte) {
	var changeBuffer *bytes.Buffer = &bytes.Buffer{}

	startY := CheckValueForBounds(inData.StartY, inField.Height)
	endY := CheckValueForBounds(inData.EndY, inField.Height)
	startX := CheckValueForBounds(inData.StartX, inField.Width)
	endX := CheckValueForBounds(inData.EndX, inField.Width)

	if startY > endY {
		temp := startY
		startY = endY
		endY = temp
	}

	if startX > endX {
		temp := startX
		startX = endX
		endX = temp
	}

	var i int64
	var step float64 = float64(endX-startX) / float64(endY-startY)
	var currentX float64 = float64(startX)

	changeBuffer.WriteString("{\"data\":[")
	inField.fieldMutex.Lock()
	for i = startY; i <= endY; i++ {
		inField.Rows[i].AddData(int64(currentX+step*float64(i)), inData.PlayerId, changeBuffer, i == endY)
	}
	changeBuffer.WriteString("]}")
	inField.fieldMutex.Unlock()
	return changeBuffer.Bytes()
}

func CheckValueForBounds(inValue int64, inMax int64) (outValue int64) {
	outValue = inValue
	if outValue < 0 {
		outValue = 0
	}
	if outValue >= inMax {
		outValue = inMax - 1
	}
	return outValue
}

type GameFieldRow struct {
	Loc      []*GameFieldLoc
	width    int64
	stroke   int64
	rowIndex int64
	hasData  bool
}

func CreateGameFieldRow(inWidth int64, inStroke int64, inRowIndex int64) (outRow *GameFieldRow) {
	outRow = &GameFieldRow{}
	outRow.width = inWidth
	outRow.rowIndex = inRowIndex
	outRow.stroke = inStroke
	outRow.Loc = make([]*GameFieldLoc, inWidth)
	var i int64
	for i = 0; i < inWidth; i++ {
		outRow.Loc[i] = CreateGameFieldLoc()
	}
	return outRow
}

func (inRow *GameFieldRow) GenerateOutput(inBuffer *bytes.Buffer, isLast bool) {
	var started bool = false
	var i int64

	var startIndex int64
	var startValue string

	var lastIndex int64
	for i = inRow.width - 1; i >= 0; i-- {
		if inRow.Loc[i].PlayerId != "" {
			lastIndex = i
			break
		}
	}

	for i = 0; i < lastIndex; i++ {
		if started {
			if inRow.Loc[i].PlayerId != startValue {
				inBuffer.WriteString(fmt.Sprintf("{\"row:\":%d,\"min\":%d,\"max\":%d,\"id\":\"%s\"},", inRow.rowIndex, startIndex, i, startValue))
				started = false
			}
		}
		if !started && inRow.Loc[i].PlayerId != "" {
			startValue = inRow.Loc[i].PlayerId
			startIndex = i
			started = true
		}
	}

	if started {
		inBuffer.WriteString(fmt.Sprintf("{\"row\":%d,\"min\":%d,\"max\":%d,\"id\":\"%s\"}", inRow.rowIndex, startIndex, i, startValue))
		if !isLast {
			inBuffer.WriteString(",")
		}
	}
}

func (inRow *GameFieldRow) AddData(inTargetX int64, inOwner string, inChangeBuffer *bytes.Buffer, inLastRow bool) {
	inRow.hasData = true
	startX := inTargetX - inRow.stroke/2 - 1
	endX := inTargetX + inRow.stroke/2 + 1
	startX = CheckValueForBounds(startX, inRow.width)
	endX = CheckValueForBounds(endX, inRow.width)

	var i int64
	for i = startX; i < endX; i++ {
		inRow.Loc[i].SetPlayer(inOwner)
	}

	inChangeBuffer.WriteString(fmt.Sprintf("{\"row\":%d,\"min\":%d,\"max\":%d,\"id\":\"%s\"}", inRow.rowIndex, startX, endX, inOwner))
	if !inLastRow {
		inChangeBuffer.WriteString(",")
	}
}

type GameFieldLoc struct {
	PlayerId string
}

func CreateGameFieldLoc() (outValue *GameFieldLoc) {
	outValue = &GameFieldLoc{}
	return outValue
}

func (inData *GameFieldLoc) SetPlayer(inPlayer string) {
	inData.PlayerId = inPlayer
}
