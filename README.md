# OpenLaserTag
OperLaserTag is a DIY system for the popular game lasertag. I have designed it for high flexibility for game modes. With this article series and a little knowledge about electronics with microcontrollers like the Arduino you should be able to build your own lasertag system.
The work on this project isn't finished jet and I would be glad to receive some honest hints about what the system should be able to do and how this can be accomplished.

The System consists of the Arduino-based tagger, which is connected to a smartphone via bluetooth. The tagger uses an infrared LED and a lens to send a beam of modulated IR-light in the direction the tagger is pointed to. This works similar to normal IR-remotes, for example for your TV. The tagger is connected to a number of receiver modules, which are located on a vest, so that you can be hit by other players with their tagger. The Arduino informs the smartphone whenever something happens via a bluetooth-serial module and the smartphone does all the management of the game.

At the current state the tagger systems of different players don't have any contact. I designed the system, so that the game follows rules written in a XML-file and will end in a given time limit. This gives great flexibility, because you don't have to set up a communication system over the whole game field (which may be quite big). But this feature (over a limited range) could be added using 433MHz transmission modules.

For more information please visit the Wiki.
