# Snap!

This is the Seriously Necessary Adapter Plugin to enable plugins written against the
 BungeeCord or Waterfall API to load and (kinda) run on [Velocity](https://velocitypowered.com/). 👀

## How?

Simply add the Bungee plugins into the plugins folder inside the Snap plugin folder.

Snap will use it's own instance of BungeeCord's plugin manager to load the plugins 
 from there and translate BungeeCord methods, classes as well as event calls to the 
 respective Velocity ones and vice versa.

## Why?

Originally I wanted to document the Velocity equivalents to Bungee events, methods
 and classes. This evolved into the idea of writing a converter for source code which
 led me to decide to try to make a plugin which can directly load Bungee plugins.
 
Seeing as the proxies don't have too much logic that seems to have worked although
 it is definitely a lot more inefficient than just running native Velocity plugins 
 due lots of classes being in need of getting translated on the fly.
 
Technically this could be made in a way that is a lot more efficient by directly 
 modifying the Velocity or BungeeCord source code to extend the respective other
 classes but in practice that massively increases the work required to get this
 plugin running, and that's all I wanted to do for now.
 
## What works?

Most of it (hopefully). I mean that's the goal... make sure to report any issues!

## What doesn't work?

Some functionality isn't easily recreated (e.g. group handling is not a
 thing in Velocity, use [a permissions plugin](https://luckperms.net)) and of course
 anything related to hacking into Bungee-internals or packets wont work.
 Just write a Velocity plugin at that point...

Those functions not supported will throw an UnsupportedOperationException. Please report
 those including the plugin causing them on the issue tracker!
 
If you are sure that the plugin will work fine otherwise then you can have it return
 default values by setting `throw-unsupported-exception` to `false` in the `snap.conf`!

### Not supported:

- Using Bungee's **inbuilt permissions system** to set and get groups/permissions. 
  Don't! Please use LuckPerms on Velocity. (hasPermission checks work though)
- **Reconnect server functionality.** That's an inbuilt function in Bungee but better 
  suited for a plugin. The related methods will return `null` or set nothing. Instead
  of erroring.
- **Scoreboards.** Velocity doesn't have API for them and I'm not going to create a 
  packet based one. Maybe there will be a way to integrate in some plugin or Velocity 
  adds support in the future.
- Some **ProxyConfig** settings don't exist on Velocity or aren't exposed in the API so 
  they return some sensible defaults which should reflect the proxy's state.
- Registering commands after a plugin was enabled. I currently have no good way to hook
  into this besides straight up modifying the PluginManager class which I would like to
  avoid.
- Velocity plugins and Bungee plugins are not available to each other via the respective
  PluginManager APIs and as dependencies. Their classes should be accessible though.
- Some connection handling and related events might not work 100% exactly like on 
  Bungee. They are as close as possible though but if you already have to fiddle with 
  that then its best to create a standalone Velocity plugin tbh.
- Some events don't work 100% or not at all.  
  Not working: `TabCompleteEvent`, `ProxyDefineCommandEvent`, `ProxyExceptionEvent`.  
  Only partially: `ServerDisconnectEvent` (only triggers on kicks),
  `ClientConnectEvent` (uses Velocity's `LoginEvent` with `PostOrder.FIRST`)
  `ConnectionInitEvent` (uses Velocity's `LoginEvent` with `PostOrder.EARLY`)
- **Unsafe** doesn't work.

## Sounds awesome! How can I get it?

You can download the jar via [GitHub releases](https://github.com/Phoenix616/Snap/releases)
 or get builds from the latest commits from the [Minebench.de Jenkins](https://ci.minebench.de/job/Snap/).
 
## How can I support the project?

For the start trying out the plugin and reporting what other plugins work and don't work
 would already help a ton figuring out what work is still needed.
 
Of course I would also appreciate [monetary help](https://tip.phoenix616.dev) if the plugin
 has helped you transition to Velocity either by directly using it or referencing its code to 
 adapt Bungee plugins to get running on Velocity natively. (Did you know that GitHub is still 
 doubling donations to [my GitHub Sponsors page](https://ghsponsor.phoenix616.dev)? 😉)

## Is it open source?

Yes, the base code of Snap is open source! Unless noted otherwise in the source it's licensed 
 under LGPLv3 in order to be compatible with the shipped Waterfall/BungeeCord.

```
 Snap
 Copyright (c) 2020 Max Lee aka Phoenix616 (max@themoep.de)

 This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation, either
 version 3 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this program.  If not, see <http://www.gnu.org/licenses/>.
```

Please note that BungeeCord is licensed under their own, BSD 3-Clause based
 [license](https://github.com/SpigotMC/BungeeCord/blob/master/LICENSE) and that Waterfall uses
 an [MIT License](https://github.com/PaperMC/Waterfall/blob/master/LICENSE.txt) for its patches.

Therefore pre-built binaries of Snap would have to be  distributed under Bungee's modified BSD 3-Clause license or a compatible one.
