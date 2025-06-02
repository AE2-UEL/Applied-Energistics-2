# Applied Energistics 2 Unofficial Extended Life

[![Downloads](http://cf.way2muchnoise.eu/570458.svg)](https://www.curseforge.com/minecraft/mc-mods/ae2-extended-life) [![MCVersion](http://cf.way2muchnoise.eu/versions/570458.svg)](https://www.curseforge.com/minecraft/mc-mods/ae2-extended-life) [![GitHub issues](https://img.shields.io/github/issues/AE2-UEL/Applied-Energistics-2.svg)](https://github.com/AE2-UEL/Applied-Energistics-2/issues) [![GitHub pull requests](https://img.shields.io/github/issues-pr/AE2-UEL/Applied-Energistics-2.svg)](https://github.com/AE2-UEL/Applied-Energistics-2/pulls)

---

## About

A maintained fork of a Mod about Matter, Energy and using them to conquer the world.

This project started as a fork of [Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2) by [PrototypeTrousers](https://github.com/PrototypeTrousers), initially addressing issues related to the [Nomifactory](https://www.curseforge.com/minecraft/modpacks/nomifactory) modpack. It is now being maintained by the [AE2 Unofficial Extended Life Team](https://github.com/AE2-UEL).

See [Releases](https://github.com/PrototypeTrousers/Applied-Energistics-2/releases) for the compiled jars.

## Loot Tables
Meteorite loot is now handled through loot tables, allowing pack developers to customize and/or replace it. 
See the [README](/src/main/java/appeng/loot/README.md) in `loot` for more details.

## RecipeStages

You need to add the following into a CraftTweaker script:
```
mods.recipestages.Recipes.setPackageStage("appeng", allStages);
```

The second argument allows you to customize which stages exactly you want to be craftable with AE2. Not sure why you would do that, but we support that nonetheless!

## License
* Applied Energistics 2 API
  - (c) 2013 - 2018 AlgorithmX2 et al
  - [![License](https://img.shields.io/badge/License-MIT-red.svg?style=flat-square)](http://opensource.org/licenses/MIT)
* Applied Energistics 2
  - (c) 2013 - 2018 AlgorithmX2 et al
  - [![License](https://img.shields.io/badge/License-LGPLv3-blue.svg?style=flat-square)](https://raw.githubusercontent.com/AppliedEnergistics/Applied-Energistics-2/rv2/LICENSE)
* Textures and Models
  - (c) 2013 - 2018 AlgorithmX2 et al
  - [![License](https://img.shields.io/badge/License-CC%20BY--NC--SA%203.0-yellow.svg?style=flat-square)](https://creativecommons.org/licenses/by-nc-sa/3.0/)
* Text and Translations
  - [![License](https://img.shields.io/badge/License-No%20Restriction-green.svg?style=flat-square)](https://creativecommons.org/publicdomain/zero/1.0/)

## Credits

Special thanks to:

* PrototypeTrousers for the initial fork
* Notch et al for Minecraft
* Lex et al for MinecraftForge
* AlgorithmX2 for AppliedEnergistics2
* all [contributors](https://github.com/AppliedEnergistics/Applied-Energistics-2/graphs/contributors)
