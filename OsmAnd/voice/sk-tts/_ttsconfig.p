:- op('==', xfy, 500).
version(100).
language(pl).

% before each announcement (beep)
preamble - [].

%% TURNS 
turn('left', ['skręć w lewo ']).
turn('left_sh', ['skręć ostro w lewo ']).
turn('left_sl', ['skręć lekko w lewo ']).
turn('right', ['skręć w prawo ']).
turn('right_sh', ['skręć ostro w prawo ']).
turn('right_sl', ['skręć lekko w prawo ']).

prepare_turn(Turn, Dist) == ['Za ', D, M] :- 
			distance(Dist) == D, turn(Turn, M).
turn(Turn, Dist) == ['Za ', D, M] :- 
			distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).


prepare_make_ut(Dist) == ['Za ', D, ' zawróć'] :- 
		distance(Dist) == D.

prepare_roundabout(Dist) == ['Za ', D, ' wjedź na rondo'] :- 
		distance(Dist) == D.

make_ut(Dist) ==  ['Za ', D, ' zawróć'] :-
			distance(Dist) == D.
make_ut == ['Zawróć '].

roundabout(Dist, _Angle, Exit) == ['Za ', D, ' wjedź na rondo ', E, 'wyjazd'] :- distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == [ E, ' wyjazd'] :- nth(Exit, E).

and_arrive_destination == ['następnie dojedź do celu ']. % Miss and?
then == ['następnie '].
reached_destination == ['Cel został osiągnięty! '].
bear_right == ['trzymaj się prawej '].
bear_left == ['trzymaj się lewej '].
route_recalc(_Dist) == []. % ['Wyznaczam nową trasę '].  %nothing to said possibly beep?	
route_new_calc(Dist) == ['Długość trasy to ', D] :- distance(Dist) == D. % nothing to said possibly beep?

go_ahead(Dist) == ['Jedź prosto ', D]:- distance(Dist) == D.
go_ahead == ['Jedź prosto '].

%% 
nth(1, 'pierwszy ').
nth(2, 'drugi ').
nth(3, 'trzeci ').
nth(4, 'czwarty ').
nth(5, 'piąty ').
nth(6, 'szósty ').
nth(7, 'siódmy ').  
nth(8, 'ósmy ').
nth(9, 'dziewiąty ').
nth(10, 'dziesiąty ').
nth(11, 'jedenasty ').
nth(12, 'dwunasty ').
nth(13, 'trzynasty ').
nth(14, 'czternasty ').
nth(15, 'piętnasty ').
nth(16, 'szestasty ').
nth(17, 'siedemnasty ').


%%% distance measure
distance(Dist) == T :- Dist < 1000, dist(Dist, F), append(F, ' metrów',T).
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

distance(Dist) == ['około jeden kilometr '] :- Dist < 1500.
distance(Dist) == ['około dwa kilometry '] :- Dist < 2500.
distance(Dist) == ['około trzy kilometry '] :- Dist < 3500.
distance(Dist) == ['około cztery kilometry '] :- Dist < 4500.
distance(Dist) == ['około pięć kilometrów '] :- Dist < 5500.
distance(Dist) == ['około sześć kilometrów '] :- Dist < 6500.
distance(Dist) == ['około siedem kilometrów '] :- Dist < 7500.
distance(Dist) == ['około osiem kilometrów '] :- Dist < 8500.
distance(Dist) == ['około dziewięć kilometrów '] :- Dist < 9500.
distance(Dist) == ['około ', X, ' kilometrów '] :- D is Dist/1000, dist(D, X).

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
