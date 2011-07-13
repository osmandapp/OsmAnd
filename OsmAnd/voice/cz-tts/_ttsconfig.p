:- op('==', xfy, 500).
version(100).
language(cz).


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
distance(Dist) == T :- Dist < 1000, dist(Dist, F), append(F, 'metrů',T).
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

distance(Dist) == ['přibližně jeden kilometr'] :- Dist < 1500.
distance(Dist) == ['přibližně 2 kilometry'] :- Dist < 2500.
distance(Dist) == ['přibližně 3 kilometry'] :- Dist < 3500.
distance(Dist) == ['přibližně 4 kilometry'] :- Dist < 4500.
distance(Dist) == ['přibližně 5 kilometrů'] :- Dist < 5500.
distance(Dist) == ['přibližně 6 kilometrů'] :- Dist < 6500.
distance(Dist) == ['přibližně 7 kilometrů'] :- Dist < 7500.
distance(Dist) == ['přibližně 8 kilometrů'] :- Dist < 8500.
distance(Dist) == ['přibližně 9 kilometrů'] :- Dist < 9500.
distance(Dist) == ['přibližně', X, 'kilometrů'] :- D is Dist/1000, dist(D, X).

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
