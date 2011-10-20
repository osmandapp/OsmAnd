:- op('==', xfy, 500).
version(101).
language(de).

% before each announcement (beep)
preamble - [].


%% TURNS 
turn('left', ['links abbiegen ']).
turn('left_sh', ['scharf links abbiegen ']).
turn('left_sl', ['leicht nach links abbiegen ']).
turn('right', ['rechts abbiegen ']).
turn('right_sh', ['scharf rechts abbiegen ']).
turn('right_sl', ['leicht nach rechts abbiegen ']).

prepare_turn(Turn, Dist) == ['Nach ', D, M] :- 
			distance(Dist, dativ) == D, turn(Turn, M).
turn(Turn, Dist) == ['Nach ', D, M] :- 
			distance(Dist, dativ) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).


prepare_make_ut(Dist) == ['Vorbereiten zum Wenden nach ', D] :- 
		distance(Dist, dativ) == D.

prepare_roundabout(Dist) == ['Einbiegen in Kreisverkehr nach ', D] :- 
		distance(Dist, dativ) == D.

make_ut(Dist) == ['Nach ', D, ' wenden '] :- 
			distance(Dist, dativ) == D.
make_ut == ['Bitte wenden '].

roundabout(Dist, _Angle, Exit) == ['Nach ', D, ' in den Kreisverkehr einfahren, dann nehmen Sie die ', E, 'Ausfahrt'] :- distance(Dist, dativ) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['nehmen Sie die ', E, 'Ausfahrt'] :- nth(Exit, E).

and_arrive_destination == ['und kommen an Ihrem Ziel an ']. % Miss and?
then == ['dann '].
reached_destination == ['Ziel erreicht '].
bear_right == ['rechts halten '].
bear_left == ['links halten '].
route_recalc(_Dist) == []. % ['Route wird neu berechnet '].  %nothing to said possibly beep?	
route_new_calc(Dist) == ['Die berechnete Strecke ist ', D, ' lang'] :- distance(Dist, nominativ) == D. % nothing to said possibly beep?

go_ahead(Dist) == ['Fahren Sie für ', D]:- distance(Dist, nominativ) == D.
go_ahead == ['Weiter geradeaus '].

%% 
nth(1, 'erste ').
nth(2, 'zweite ').
nth(3, 'dritte ').
nth(4, 'vierte ').
nth(5, 'fünfte ').
nth(6, 'sechste ').
nth(7, 'siebte ').
nth(8, 'achte ').
nth(9, 'neunte ').
nth(10, 'zehnte ').
nth(11, 'elfte ').
nth(12, 'zwölfte ').
nth(13, 'dreizehnte ').
nth(14, 'vierzehnte ').
nth(15, 'fünfzehnte ').
nth(16, 'sechzehnte ').
nth(17, 'siebzehnte ').

%%% distance measure
distance(Dist, nominativ) == [ X, ' meter'] :- Dist < 100, D is round(Dist/10)*10, num_atom(D, X).
distance(Dist, dativ) == [ X, ' metern'] :- Dist < 100, D is round(Dist/10)*10, num_atom(D, X).
distance(Dist, nominativ) == [ X, ' meter'] :- Dist < 1000, D is round(2*Dist/100)*50, num_atom(D, X).
distance(Dist, dativ) == [ X, ' metern'] :- Dist < 1000, D is round(2*Dist/100)*50, num_atom(D, X).
distance(Dist, nominativ) == ['zirka einen Kilometer '] :- Dist < 1500.
distance(Dist, dativ) == ['zirka einem Kilometer '] :- Dist < 1500.
distance(Dist, nominativ) == ['zirka ', X, ' Kilometer '] :- Dist < 10000, D is round(Dist/1000), num_atom(D, X).
distance(Dist, dativ) == ['zirka ', X, 'Kilometern '] :- Dist < 10000, D is round(Dist/1000), num_atom(D, X).
distance(Dist, nominativ) == [ X, ' Kilometer '] :- D is round(Dist/1000), num_atom(D, X).
distance(Dist, dativ) == [ X, 'Kilometern '] :- D is round(Dist/1000), num_atom(D, X).

%% resolve command main method
%% if you are familar with Prolog you can input specific to the whole mechanism,
%% by adding exception cases.
flatten(X, Y) :- flatten(X, [], Y), !.
flatten([], Acc, Acc).
flatten([X|Y], Acc, Res):- 
		flatten(Y, Acc, R), flatten(X, R, Res).
flatten(X, Acc, [X|Acc]).

resolve(X, Y) :- resolve_impl(X,Z), flatten(Z, Y).
resolve_impl([],[]).
resolve_impl([X|Rest], List) :- resolve_impl(Rest, Tail), ((X == L) -> append(L, Tail, List); List = Tail).
   