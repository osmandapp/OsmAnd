:- op('==', xfy, 500).
version(101).
language(it).

% before each announcement (beep)
preamble - [].

%% TURNS 
turn('left', ['girate a sinistra ']).
turn('left_sh', ['subito a sinistra ']).
turn('left_sl', ['girate leggermente a sinistra ']).
turn('right', ['girate a destra ']).
turn('right_sh', ['subito a destra ']).
turn('right_sl', ['girate leggermente a destra ']).

prepare_turn(Turn, Dist) == ['Dopo', D,' si preparano a ', M] :- 
   distance(Dist) == D, turn(Turn, M).
turn(Turn, Dist) == ['Dopo ', D, M] :- 
   distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).

prepare_make_ut(Dist) == [ 'Dopo ', D,' inversione a u'] :- 
   distance(Dist) == D.

prepare_roundabout(Dist) == [ 'Dopo ', D,' si preparano a entrare una rotonda '] :- 
   distance(Dist) == D.

make_ut(Dist) == [' Dopo', D, ' inversione a u'] :- 
   distance(Dist) == D.
make_ut == ['Inversione a u'].

roundabout(Dist, _Angle, Exit) == ['Dopo ', D, ' entrare la rotonda e prendete la ', 
   E ] :- distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['prendete la ', E ] :- nth(Exit, E).

and_arrive_destination == ['e arrivate a destinazione'].
then == ['Dopo '].
reached_destination == ['arrivato a destinazione'].
bear_right == ['spostatevi su la destra'].
bear_left == ['spostatevi su la sinistra'].
route_recalc(_Dist) == []. % ['recalculating route ']. nothing to said possibly beep?
route_new_calc(_Dist) == ['Il viaggio è ', D] :- distance(Dist) == D. % nothing to said possibly beep?	

go_ahead(Dist) == ['Sempre dritto per ',  D]:- distance(Dist) == D.
go_ahead == ['Sempre dritto '].

%% 
nth(1, 'prima uscita').
nth(2, 'seconda uscita').
nth(3, 'terza uscita').
nth(4, 'quarta uscita').
nth(5, 'quinta uscita').
nth(6, 'sexsta uscita').
nth(7, 'uscita settimo').
nth(8, 'ottava uscita').
nth(9, 'uscita nono').
nth(10, 'uscita decimo').
nth(11, 'uscita undicesimo').
nth(12, 'dodicesima uscita').
nth(13, 'tredicesima uscita').
nth(14, 'uscita quattordicesima').
nth(15, 'quindicesima uscita').
nth(16, 'uscita sedicesimo').
nth(17, 'uscita deciassettesimo').

%%% distance measure
distance(Dist) == [ X, ' metri'] :- Dist < 100, D is round(Dist/10)*10, num_atom(D, X).
distance(Dist) == [ X, ' metri'] :- Dist < 1000, D is round(2*Dist/100)*50, num_atom(D, X).
distance(Dist) == ['circa un chilometro '] :- Dist < 1500.
distance(Dist) == ['circa ', X, ' chilometri '] :- Dist < 10000, D is round(Dist/1000), num_atom(D, X).
distance(Dist) == [ X, ' chilometri '] :- D is round(Dist/1000), num_atom(D, X).

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