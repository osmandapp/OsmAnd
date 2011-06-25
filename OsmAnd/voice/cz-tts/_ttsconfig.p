:- op('==', xfy, 500).
version(100).
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
distance(Dist) == T :- Dist < 1000, dist(Dist, F), append(F, 'metrů',T).
dist(D, ['10']) :-  D < 20, !.
dist(D, ['20']) :-  D < 30, !.
dist(D, ['30']) :-  D < 40, !.
dist(D, ['40']) :-  D < 50, !.
dist(D, ['50']) :-  D < 60, !.
dist(D, ['60']) :-  D < 70, !.
dist(D, ['70']) :-  D < 80, !.
dist(D, ['80']) :-  D < 90, !.
dist(D, ['90']) :-  D < 100, !.
dist(D, ['100']) :-  D < 150, !.
dist(D, ['150']) :-  D < 200, !.
dist(D, ['200']) :-  D < 250, !.
dist(D, ['250']) :-  D < 300, !.
dist(D, ['300']) :-  D < 350, !.
dist(D, ['350']) :-  D < 400, !.
dist(D, ['400']) :-  D < 450, !.
dist(D, ['450']) :-  D < 500, !.
dist(D, ['500']) :-  D < 550, !.
dist(D, ['550']) :-  D < 600, !.
dist(D, ['600']) :-  D < 650, !.
dist(D, ['650']) :-  D < 700, !.
dist(D, ['700']) :-  D < 750, !.
dist(D, ['750']) :-  D < 800, !.
dist(D, ['800']) :-  D < 850, !.
dist(D, ['850']) :-  D < 900, !.
dist(D, ['900']) :-  D < 950, !.
dist(D, ['950']) :-  !.


distance(Dist) == ['více jak jeden kilometr'] :- Dist < 1500.
distance(Dist) == ['více jak 2 kilometry'] :- Dist < 3000.
distance(Dist) == ['více jak 3 kilometry'] :- Dist < 4000.
distance(Dist) == ['více jak 4 kilometry'] :- Dist < 5000.
distance(Dist) == ['více jak 5 kilometrů'] :- Dist < 6000.
distance(Dist) == ['více jak 6 kilometrů'] :- Dist < 7000.
distance(Dist) == ['více jak 7 kilometrů'] :- Dist < 8000.
distance(Dist) == ['více jak 8 kilometrů'] :- Dist < 9000.
distance(Dist) == ['více jak 9 kilometrů'] :- Dist < 10000.
distance(Dist) == ['více jak', X, 'kilometrů'] :- D is Dist/1000, dist(D, X).



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
