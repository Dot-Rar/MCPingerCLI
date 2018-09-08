# What is this?
MCPingerCLI is a simple command tool to ping Minecraft servers and retrieve some simple information about said server

# Running
1) Download MCPingerCLI from [here](https://github.com/Dot-Rar/MCPingerCLI/releases)
2) Run
```
java -jar MCPingerCLI.jar
```
3) Enter the Minecraft server IP when prompted. Ports are supported using the format host:port

# Example Response
```
success: true
motd: Perkelle | TNT Wars | Purchasable cannons out now!
Party & friends update out now!
hostname: perkelle.com
protocol: 4
maxPlayers: 800
port: 25565
playerCount: 3
ping: 212
online: true
versionName: BungeeCord 1.8.x-1.13.x
```

# Building
```bash
git clone https://github.com/Dot-Rar/MCPingerCLI.git
cd MCPingerCLI
gradle build
cd build/libs
```