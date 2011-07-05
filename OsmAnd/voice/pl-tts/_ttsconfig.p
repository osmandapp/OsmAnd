:- op('==', xfy, 500).
version(100).
language(pl).

% before each announcement (beep)
preamble - [].

%% TURNS 
turn('left', ['skręć w lewo ']).
turn('left_sh', ['ostro w lewo ']).
turn('left_sl', ['lekko w lewo ']).
turn('right', ['skręć w prawo ']).
turn('right_sh', ['ostro w prawo ']).
turn('right_sl', ['lekko w prawo ']).

prepare_turn(Turn, Dist) == ['Za ', D, ' ', M] :- 
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
route_recalc(_Dist) == []. % ['Wyznaczam nową drogę '].  %nothing to said possibly beep?	
route_new_calc(Dist) == ['Odległość wynosi ', D] :- distance(Dist) == D. % nothing to said possibly beep?

go_ahead(Dist) == ['Jedź tą drogą ', D]:- distance(Dist) == D.
go_ahead == ['Jedź tą drogą '].

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

distance(Dist) == ['ponad jeden kilometr '] :- Dist < 1500.
distance(Dist) == ['ponad dwa kilometry '] :- Dist < 3000.
distance(Dist) == ['ponad trzy kilometry '] :- Dist < 4000.
distance(Dist) == ['ponad cztery kilometry '] :- Dist < 5000.
distance(Dist) == ['ponad pięć kilometrów '] :- Dist < 6000.
distance(Dist) == ['ponad sześć kilometrów '] :- Dist < 7000.
distance(Dist) == ['ponad siedem kilometrów '] :- Dist < 8000.
distance(Dist) == ['ponad osiem kilometrów '] :- Dist < 9000.
distance(Dist) == ['ponad dziewięć kilometrów '] :- Dist < 10000.
distance(Dist) == ['ponad ', X, ' kilometrów '] :- D is Dist/1000, dist(D, X).

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
