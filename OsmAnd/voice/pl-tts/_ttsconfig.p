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
distance(Dist) == [ X, ' metrów'] :- Dist < 100, D is round(Dist/10)*10, num_atom(D, X).
distance(Dist) == [ X, ' metrów'] :- Dist < 1000, D is round(2*Dist/100)*50, num_atom(D, X).
distance(Dist) == ['około jeden kilometr '] :- Dist < 1500.
distance(Dist) == ['około ', X, ' kilometry '] :- Dist < 4500, D is round(Dist/1000), num_atom(D, X).
distance(Dist) == ['około ', X, ' kilometrów '] :- Dist < 10000, D is round(Dist/1000), num_atom(D, X).
distance(Dist) == [ X, ' kilometrów '] :- D is round(Dist/1000), num_atom(D, X).

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
