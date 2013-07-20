********************************
*                              *
*            PARMS             *
*                              *
* for Merlin - ProDOS version. *
*                              *
*   Glen E. Bredon   8/11/85   *
*                              *
********************************

SAVOBJ	KBD	"Save	the	object	code?	(1=Y,	0=N)"

DATA
	DFB	60	;# lines/page for PRTR
	DFB	0	;Page skip (formfeed if 0)
	DFB	80	;# printer columns
	DFB	$80	;- if printer does CR at
; end of # columns

	DFB	$83	;80 col flag (DO NOT CHANGE)
;(Except V-bit which will
; cause ProDOS to be moved to
; aux memory and Merlin to
; load into main memory.
; I.e., use $C3 for this.)
*-------------------------------------------------
* Source address must be above $8A0.  We use $901
* to maintain compatibility.  It can be set higher
* to protect an area of memory for any purpose:
*-------------------------------------------------
SOURCE	=	$901
	DA	SOURCE	;Start of source
	DA	$AA00	;Don't change this
	DA	SOURCE	;End of source

	DFB	<"^"	;Editor's wild card
	DFB	4	;# of symbol columns
	DFB	0	;Search chr for "Update..."
; (Question skipped if 0)

	DFB	14,20,31	;Default tabs

	DFB	8	;# obj bytes/line after 1st

	DFB	$45	;High bit neg. forces infinite
; wait for key upon assembly
; errors.
;V-bit set defeats bells.
;Low nibble is Ultraterm entry
; mode (e.g., 5, $45, $85 or $C5
; gives 32x80 interlace mode)

	DFB	0	;Regular cursor if $40
;Block cursor if 0
;(Ignored if Apple card)

	DFB	0	;Default for LSTDO opcode:
;Do off areas listed if 0 or 1,
; not if >1.
;Bit 0, if clear, causes shift
; to 40 columns on a PRTR1 cmd

	DFB	80-8	;Column for cycle count

	DFB	$EC	;Ultraterm cursor in default
; mode.  (This must be changed
; if the ultraterm mode is
; changed.)

	ERR	*-DATA-23 ;23 data bytes to here.

* User file type names (change to fit your desires):

	ASC	"$F1"
	ASC	"$F2"
	ASC	"$F3"
	ASC	"$F4"
	ASC	"$F5"
	ASC	"$F6"
	ASC	"$F7"

	ERR	*-DATA-44 ;44 bytes in all
; no more, no less

*----------------------------------------------
* Be sure to change the following pathname if
* you want this on a volume with another name.
* The PARMS file must be in the MAIN DIRECTORY!
*----------------------------------------------

	DO	SAVOBJ
	SAV	/LL/PARMS
	FIN
