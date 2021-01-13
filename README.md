# Battlecode2021
Codebase for Battlecode 2021 competition

##Flags and opcodes

###Politician:

####Scout:

Scout can broadcast boundary coordinates and EC location coordinates.

####Troop:

Troop checks for EC location coordinates and if it finds it, broadcasts location w/ probability 1/2 and 
broadcasts ID of original EC with probability 1/2 (to help new ECs).

####Defender:

Same as slanderer.

###Slanderer:

Flag is set to a SENDECID opcode, broadcasting the ID of its original EC. 
This is to help newly captured ECs communicate with existing ECs.

###Muckraker:

Same as slanderer.
