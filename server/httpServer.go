package main

import (
	"fmt"
	"net/http"
	"sync"

	"golang.org/x/net/websocket"
)

const (
	OUTPUT_CHAN_SIZE int64 = 1000
)

type ServiceHTTPServer struct {
	handler     *Application
	httpBinding string

	channelMutex   *sync.RWMutex
	outputChannels []chan []byte
	httpServer     *http.Server
	clientSema     chan struct{}

	inputHandler  websocket.Handler
	outputHandler websocket.Handler
}

func CreateServiceHTTPServer(inBinding string, inMaxClients int64) (outServer *ServiceHTTPServer) {
	outServer = &ServiceHTTPServer{}
	outServer.clientSema = make(chan struct{}, inMaxClients)
	outServer.httpBinding = inBinding
	outServer.channelMutex = &sync.RWMutex{}
	outServer.inputHandler = websocket.Handler(outServer.handleInput)
	outServer.outputHandler = websocket.Handler(outServer.handleOutput)
	return outServer
}

func (inServer *ServiceHTTPServer) SetHandler(inHandler *Application) {
	inServer.handler = inHandler
}

func (inServer *ServiceHTTPServer) AddDataToSend(inData []byte) {
	length := len(inServer.outputChannels)
	inServer.channelMutex.Lock()
	for i := 0; i < length; i++ {
		if inServer.outputChannels[i] != nil && len(inServer.outputChannels[i]) < int(OUTPUT_CHAN_SIZE)-1 {
			inServer.outputChannels[i] <- inData
		} else {
			if inServer.outputChannels[i] != nil {
				close(inServer.outputChannels[i])
				inServer.outputChannels[i] = nil
			}
			inServer.outputChannels = append(inServer.outputChannels[:i], inServer.outputChannels[i+1:]...)
		}
	}
	inServer.channelMutex.Unlock()
}

func (inServer *ServiceHTTPServer) handleInput(ws *websocket.Conn) {
	var data []byte
	for {
		data = make([]byte, 1024)
		n, err := ws.Read(data)
		data = data[0:n]
		if err != nil {
			fmt.Printf("Error reading input from %s. Error: %s.\n", ws.RemoteAddr().String(), err.Error())
			break
		} else {
			inServer.handler.AddInputData(data)
		}
	}
	fmt.Printf("Closing input connection with %s.\n", ws.RemoteAddr().String())
}

func (inServer *ServiceHTTPServer) handleOutput(ws *websocket.Conn) {
	var outputChannel chan []byte = make(chan []byte, OUTPUT_CHAN_SIZE)
	inServer.channelMutex.Lock()
	inServer.outputChannels = append(inServer.outputChannels, outputChannel)
	inServer.channelMutex.Unlock()

	var err error
	data := inServer.handler.GetGamefieldData()
	_, err = ws.Write(data)
	if err == nil {
		var ok bool
		for {
			data, ok = <-outputChannel
			if ok {
				_, err = ws.Write(data)
				if err != nil {
					fmt.Printf("Error sending data to %s. Error: %s. Closing socket.\n", ws.RemoteAddr().String(), err.Error())
					break
				}
			} else {
				break
			}
		}
	} else {
		fmt.Printf("Failed to send initial data to %s. Error: %s.", ws.RemoteAddr().String(), err.Error())
	}
	ws.Close()
}

func (inServer *ServiceHTTPServer) StartHTTPServer() {
	inServer.httpServer = &http.Server{}
	inServer.httpServer.Addr = inServer.httpBinding
	inServer.httpServer.Handler = inServer
	go inServer.httpServer.ListenAndServe()
}

func (inServer *ServiceHTTPServer) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	inServer.clientSema <- struct{}{}
	defer func() { <-inServer.clientSema }()

	path := r.URL.Path

	if path == "/input" {
		inServer.inputHandler.ServeHTTP(w, r)
	} else if path == "/output" {
		inServer.outputHandler.ServeHTTP(w, r)
	}
}
