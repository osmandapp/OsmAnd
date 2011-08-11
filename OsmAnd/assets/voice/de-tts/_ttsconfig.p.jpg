:- op('==', xfy, 500).
version(100).
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
distance(Dist, nominativ) == T :- Dist < 1000, dist(Dist, F), append(F, ' meter ',T).
distance(Dist, dativ) == T :- Dist < 1000, dist(Dist, F), append(F, ' metern ',T).
dist(D, ['10 ']) :-  D < 15, !.
dist(D, ['20 ']) :-  D < 25, !.
dist(D, ['30 ']) :-  D < 35, !.
dist(D, ['40 ']) :-  D < 45, !.
dist(D, ['50 ']) :-  D < 55, !.
dist(D, ['60 ']) :-  D < 65, !.
dist(D, ['70 ']) :-  D < 75, !.
dist(D, ['80 ']) :-  D < 85, !.
dist(D, ['90 ']) :-  D < 95, !.
dist(D, ['100 ']) :-  D < 125, !.
dist(D, ['150 ']) :-  D < 175, !.
dist(D, ['200 ']) :-  D < 225, !.
dist(D, ['250 ']) :-  D < 275, !.
dist(D, ['300 ']) :-  D < 325, !.
dist(D, ['350 ']) :-  D < 375, !.
dist(D, ['400 ']) :-  D < 425, !.
dist(D, ['450 ']) :-  D < 475, !.
dist(D, ['500 ']) :-  D < 525, !.
dist(D, ['550 ']) :-  D < 575, !.
dist(D, ['600 ']) :-  D < 625, !.
dist(D, ['650 ']) :-  D < 675, !.
dist(D, ['700 ']) :-  D < 725, !.
dist(D, ['750 ']) :-  D < 775, !.
dist(D, ['800 ']) :-  D < 825, !.
dist(D, ['850 ']) :-  D < 875, !.
dist(D, ['900 ']) :-  D < 925, !.
dist(D, ['950 ']) :-  D < 975, !.
dist(D, ['1000 ']) :-  !.

distance(Dist, nominativ) == ['zirka einen Kilometer'] :- Dist < 1500.
distance(Dist, dativ) == ['zirka einem Kilometer '] :- Dist < 1500.
distance(Dist, nominativ) == ['zirka 2 Kilometer '] :- Dist < 2500.
distance(Dist, dativ) == ['zirka 2 Kilometern '] :- Dist < 2500.
distance(Dist, nominativ) == ['zirka 3 Kilometer '] :- Dist < 3500.
distance(Dist, dativ) == ['zirka 3 Kilometern '] :- Dist < 3500.
distance(Dist, nominativ) == ['zirka 4 Kilometer '] :- Dist < 4500.
distance(Dist, dativ) == ['zirka 4 Kilometern '] :- Dist < 4500.
distance(Dist, nominativ) == ['zirka 5 Kilometer '] :- Dist < 5500.
distance(Dist, dativ) == ['zirka 5 Kilometern '] :- Dist < 5500.
distance(Dist, nominativ) == ['zirka 6 Kilometer '] :- Dist < 6500.
distance(Dist, dativ) == ['zirka 6 Kilometern '] :- Dist < 6500.
distance(Dist, nominativ) == ['zirka 7 Kilometer '] :- Dist < 7500.
distance(Dist, dativ) == ['zirka 7 Kilometern '] :- Dist < 7500.
distance(Dist, nominativ) == ['zirka 8 Kilometer '] :- Dist < 8500.
distance(Dist, dativ) == ['zirka 8 Kilometern '] :- Dist < 8500.
distance(Dist, nominativ) == ['zirka 9 Kilometer '] :- Dist < 9500.
distance(Dist, dativ) == ['zirka 9 Kilometern '] :- Dist < 9500.
distance(Dist, nominativ) == ['zirka ', X, ' Kilometer '] :- D is Dist/1000, dist(D, X).
distance(Dist, dativ) == ['zirka ', X, ' Kilometern '] :- D is Dist/1000, dist(D, X).

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
   