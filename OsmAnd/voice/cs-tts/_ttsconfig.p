:- op('==', xfy, 500).
version(101).
language(cs).

% before each announcement (beep)
preamble - [].


%% TURNS 
turn('left', ['odbočte vlevo']).
turn('left_sh', ['odbočte ostře vlevo']).
turn('left_sl', ['odbočte mírně vlevo']).
turn('right', ['odbočte vpravo']).
turn('right_sh', ['odbočte ostře vpravo']).
turn('right_sl', ['odbočte mírně vpravo']).

pturn('left', ['vlevo']).
pturn('left_sh', ['ostře vlevo']).
pturn('left_sl', ['mírně vlevo']).
pturn('right', ['vpravo']).
pturn('right_sh', ['ostře vpravo']).
pturn('right_sl', ['mírně vpravo']).

prepare_turn(Turn, Dist) == ['o', D, 'budete odbočovat', M] :-
			distance(Dist) == D, pturn(Turn, M).
turn(Turn, Dist) == ['o', D, M] :- 
			distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).

prepare_make_ut(Dist) == ['o', D, 'se budete otáčet zpět'] :- 
		distance(Dist) == D.

prepare_roundabout(Dist) == ['o', D, 'přijedete na kruhový objezd'] :- 
		distance(Dist) == D.

make_ut(Dist) == ['o', D, 'se otočte zpět'] :- 
			distance(Dist) == D.
make_ut == ['otočte se zpět'].

roundabout(Dist, _Angle, Exit) == ['o', D, 'vjeďte na kruhový objezd', 'a zvolte', E, 'výjezd'] :- 
		distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['jděte cez', E, 'výjezd'] :- nth(Exit, E).

and_arrive_destination == ['a dorazíte do cíle']. % Miss and?
then == ['pak'].
reached_destination == ['dorazili jste do cíle'].
bear_right == ['držte se vpravo'].
bear_left == ['držte se vlevo'].
route_recalc(_Dist) == ['přepočítávám']. % nothing to said possibly beep?	
route_new_calc(Dist) == ['cesta je dlouhá', D] :- distance(Dist) == D. % nothing to said possibly beep?

go_ahead(Dist) == ['pokračujte', D]:- distance(Dist) == D.
go_ahead == ['pokračujte rovně'].

%% 
nth(1, 'první').
nth(2, 'druhý').
nth(3, 'třetí').
nth(4, 'čtvrtý').
nth(5, 'pátý').
nth(6, 'šestý').
nth(7, 'sedmý').
nth(8, 'osmý').
nth(9, 'devátý').
nth(10, 'desátý').
nth(11, 'jedenáctý').
nth(12, 'dvanáctý').
nth(13, 'třináctý').
nth(14, 'čtrnáctý').
nth(15, 'patnáctý').
nth(16, 'šestnáctý').
nth(17, 'sedmnáctý').

%%% distance measure
distance(Dist) == [ X, 'metrů'] :- Dist < 100, D is round(Dist/10)*10, num_atom(D, X).
distance(Dist) == [ X, 'metrů'] :- Dist < 1000, D is round(2*Dist/100)*50, num_atom(D, X).
distance(Dist) == ['přibližně jeden kilometr'] :- Dist < 1500.
distance(Dist) == ['přibližně', X, 'kilometry'] :- Dist < 4500, D is round(Dist/1000), num_atom(D, X).
distance(Dist) == ['přibližně', X, 'kilometrů'] :- Dist < 10000, D is round(Dist/1000), num_atom(D, X).
distance(Dist) == [ X, 'kilometrů'] :- D is round(Dist/1000), num_atom(D, X).

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
