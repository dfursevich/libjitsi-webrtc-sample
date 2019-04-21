# libjitsi-webrtc-sample

Spring boot + jitsilib webrtc sample. 

1. Run spring boot app JavaWebrtcApplication
2. Open streaming page http://localhost:8080/stream.html
3. Open watching page http://localhost:8080/watch.html. Wait for a few minutes until streaming 
video appears (a bug in implementation, chrome can't detect video format for some period of time
 see chrome://webrtc-internals/ ssrc_*_recv page)