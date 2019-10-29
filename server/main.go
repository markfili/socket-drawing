package main

import (
	"time"
)

const (
	CONFIG_FILE string = "config.ini"
	MAX_CLIENTS int64  = 500

	INPUT_CHAN_SIZE int64 = 10000

	DEFAULT_FIELD_WIDTH  int64  = 100
	DEFAULT_FIELD_HEIGHT int64  = 100
	DEFAULT_STROKE       int64  = 5
	HTTP_BINDING_DEFAULT string = ":13131"
)

type ApplicationConfig struct {
	defaultFieldWidth  int64
	defaultFieldHeight int64
	defaultStroke      int64
	httpBinding        string
}

func CreateApplicationConfig() (outConfig *ApplicationConfig) {
	outConfig = &ApplicationConfig{}
	outConfig.defaultFieldWidth = DEFAULT_FIELD_WIDTH
	outConfig.defaultFieldHeight = DEFAULT_FIELD_HEIGHT
	outConfig.defaultStroke = DEFAULT_STROKE
	outConfig.httpBinding = HTTP_BINDING_DEFAULT
	return outConfig
}

func (inConfig *ApplicationConfig) ReadFromFile(inConfigFile string) {

}

type Application struct {
	config *ApplicationConfig

	server *ServiceHTTPServer

	currentGameField *GameField

	inputDataRawChan  chan []byte
	inputDataChan     chan *InputCommand
	outputDataRawChan chan []byte
}

func CreateApplication(inConfig *ApplicationConfig) (outApp *Application) {
	outApp = &Application{}
	outApp.config = inConfig
	outApp.server = CreateServiceHTTPServer(inConfig.httpBinding, MAX_CLIENTS)
	outApp.server.SetHandler(outApp)
	outApp.inputDataRawChan = make(chan []byte, INPUT_CHAN_SIZE)
	outApp.inputDataChan = make(chan *InputCommand, INPUT_CHAN_SIZE)
	outApp.outputDataRawChan = make(chan []byte, INPUT_CHAN_SIZE)
	return outApp
}

func (inApp *Application) Init() {
	inApp.CreateNewField(inApp.config.defaultFieldWidth, inApp.config.defaultFieldHeight, inApp.config.defaultStroke)
	go inApp.parseInputData()
	go inApp.handleInputData()
	go inApp.handleOutputData()
	inApp.server.StartHTTPServer()
}

func (inApp *Application) CreateNewField(inWidth int64, inHeight int64, inStroke int64) {
	inApp.currentGameField = CreateGameField(inWidth, inHeight, inStroke)
}

func (inApp *Application) AddInputData(inNewData []byte) {
	inApp.inputDataRawChan <- inNewData
}

func (inApp *Application) parseInputData() {
	var data []byte
	var ok bool
	for {
		data, ok = <-inApp.inputDataRawChan
		if ok {
			command := GetInputCommandFromJSON(data)
			inApp.inputDataChan <- command
		} else {
			break
		}
	}
}

func (inApp *Application) handleInputData() {
	var command *InputCommand
	var ok bool
	for {
		command, ok = <-inApp.inputDataChan
		if ok {
			newData := inApp.currentGameField.AddData(command)
			inApp.outputDataRawChan <- newData
		} else {
			break
		}
	}
}

func (inApp *Application) handleOutputData() {
	var newData []byte
	var ok bool
	for {
		newData, ok = <-inApp.outputDataRawChan
		if ok {
			inApp.server.AddDataToSend(newData)
		} else {
			break
		}
	}
}

func (inApp *Application) GetGamefieldData() (outData []byte) {
	return inApp.currentGameField.GetJSON()
}

func main() {
	config := CreateApplicationConfig()
	config.ReadFromFile(CONFIG_FILE)
	app := CreateApplication(config)
	app.Init()
	for {
		time.Sleep(time.Second)
	}
}
