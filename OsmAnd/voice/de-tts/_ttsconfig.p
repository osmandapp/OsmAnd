:- op('==', xfy, 500).
version(100).
language(de).

% before each announcement (beep)
preamble - [].


%% TURNS 
turn('left', ['links abbiegen ']).
turn('left_sh', ['scharf links ']).
turn('left_sl', ['leicht nach links ']).
turn('right', ['rechts abbiegen ']).
turn('right_sh', ['scharf rechts ']).
turn('right_sl', ['leicht nach rechts ']).

prepare_turn(Turn, Dist) == ['Vorbereiten ', M, ' nach ', D] :- 
			distance(Dist) == D, turn(Turn, M).
turn(Turn, Dist) == ['Nach ', D, M] :- 
			distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).


prepare_make_ut(Dist) == ['Vorbereiten zum Drehen nach ', D, ' umdrehen'] :- 
		distance(Dist) == D.

prepare_roundabout(Dist) == ['Vorbereiten für Kreisverkehr nach  ', D] :- 
		distance(Dist) == D.

make_ut(Dist) == ['Nach ', D, ' umdrehen '] :- 
			distance(Dist) == D.
make_ut == ['Umdrehen '].

roundabout(Dist, _Angle, Exit) == ['Nach ', D, ' in den Kreisverkehr einfahren, nehmen Sie die  ', E, 'Ausfahrt'] :- distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['nehmen Sie die ', E, 'Ausfahrt'] :- nth(Exit, E).

and_arrive_destination == ['und kommen and Ihrem Ziel an ']. % Miss and?
then == ['dann '].
reached_destination == ['haben Sie Ihr Ziel erreicht '].
bear_right == ['rechts halten '].
bear_left == ['links halten '].
route_recalc(_Dist) == []. % ['Route neu berechnen '].  %nothing to said possibly beep?	
route_new_calc(Dist) == ['Die Fahrt ist ', D] :- distance(Dist) == D. % nothing to said possibly beep?

go_ahead(Dist) == ['Fahren Sie für ', D]:- distance(Dist) == D.
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
nth(12, 'zwlfte ').
nth(13, 'dreizehnte ').
nth(14, 'vierzehnte ').
nth(15, 'fünfzehnte ').
nth(16, 'sechzehnte ').
nth(17, 'siebzehnte ').


%%% distance measure
distance(Dist) == T :- Dist < 1000, dist(Dist, F), append(F, ' meters',T).
dist(D, ['10 ']) :-  D < 20, !.
dist(D, ['20 ']) :-  D < 30, !.
dist(D, ['30 ']) :-  D < 40, !.
dist(D, ['40 ']) :-  D < 50, !.
dist(D, ['50 ']) :-  D < 60, !.
dist(D, ['60 ']) :-  D < 70, !.
dist(D, ['70 ']) :-  D < 80, !.
dist(D, ['80 ']) :-  D < 90, !.
dist(D, ['90 ']) :-  D < 100, !.
dist(D, ['100 ']) :-  D < 150, !.
dist(D, ['150 ']) :-  D < 200, !.
dist(D, ['200 ']) :-  D < 250, !.
dist(D, ['250 ']) :-  D < 300, !.
dist(D, ['300 ']) :-  D < 350, !.
dist(D, ['350 ']) :-  D < 400, !.
dist(D, ['400 ']) :-  D < 450, !.
dist(D, ['450 ']) :-  D < 500, !.
dist(D, ['500 ']) :-  D < 550, !.
dist(D, ['550 ']) :-  D < 600, !.
dist(D, ['600 ']) :-  D < 650, !.
dist(D, ['650 ']) :-  D < 700, !.
dist(D, ['700 ']) :-  D < 750, !.
dist(D, ['750 ']) :-  D < 800, !.
dist(D, ['800 ']) :-  D < 850, !.
dist(D, ['850 ']) :-  D < 900, !.
dist(D, ['900 ']) :-  D < 950, !.
dist(D, ['950 ']) :-  !.

distance(Dist) == ['mehr als 1 Kilometer '] :- Dist < 1500.
distance(Dist) == ['mehr als 2 Kilometer '] :- Dist < 3000.
distance(Dist) == ['mehr als 3 Kilometer '] :- Dist < 4000.
distance(Dist) == ['mehr als 4 Kilometer '] :- Dist < 5000.
distance(Dist) == ['mehr als 5 Kilometer '] :- Dist < 6000.
distance(Dist) == ['mehr als 6 Kilometer '] :- Dist < 7000.
distance(Dist) == ['mehr als 7 Kilometer '] :- Dist < 8000.
distance(Dist) == ['mehr als 8 Kilometer '] :- Dist < 9000.
distance(Dist) == ['mehr als 9 Kilometer '] :- Dist < 10000.
distance(Dist) == ['mehr als ', X, ' Kilometer '] :- D is Dist/1000, dist(D, X).

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
   