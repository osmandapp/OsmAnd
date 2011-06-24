:- op('==', xfy, 500).
version(100).
language(sk).


% before each announcement (beep)
preamble - [].


%% TURNS 
turn('left', ['odbočte doľava']).
turn('left_sh', ['odbočte ostro doľava']).
turn('left_sl', ['odbočte mierne doľava']).
turn('right', ['odbočte doprava']).
turn('right_sh', ['odbočte ostro doprava']).
turn('right_sl', ['odbočte mierne doprava']).

pturn('left', ['doľava']).
pturn('left_sh', ['ostro doľava']).
pturn('left_sl', ['mierne doľava']).
pturn('right', ['doprava']).
pturn('right_sh', ['ostro doprava']).
pturn('right_sl', ['mierne doprava']).

prepare_turn(Turn, Dist) == ['o', D, 'budete odbáčať', M] :-
			distance(Dist) == D, pturn(Turn, M).
turn(Turn, Dist) == ['o', D, M] :- 
			distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).


prepare_make_ut(Dist) == ['o', D, 'sa budete otáčať naspäť'] :- 
		distance(Dist) == D.

prepare_roundabout(Dist) == ['o', D, 'vojdete na kruhový objazd'] :- 
		distance(Dist) == D.

make_ut(Dist) == ['o', D, 'sa otočte naspäť'] :- 
			distance(Dist) == D.
make_ut == ['otočte sa naspäť'].

roundabout(Dist, _Angle, Exit) == ['o', D, 'vojdite na kruhový objazd', 'a zvoľte', E, 'výjazd'] :- 
		distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['pôjdete cez', E, 'výjazd'] :- nth(Exit, E).

and_arrive_destination == ['a dorazíte do cieľa']. % Miss and?
then == ['potom'].
reached_destination == ['dorazili ste do cieľa'].
bear_right == ['držte sa vpravo'].
bear_left == ['držte sa vľavo'].
route_recalc(_Dist) == ['prepočítavam']. % nothing to said possibly beep?	
route_new_calc(Dist) == ['cesta je dlhá', D] :- distance(Dist) == D. % nothing to said possibly beep?

go_ahead(Dist) == ['pokračujte', D]:- distance(Dist) == D.
go_ahead == ['pokračujte rovno'].

%% 
nth(1, 'prvý').
nth(2, 'druhý').
nth(3, 'tretí').
nth(4, 'štvrtý').
nth(5, 'piaty').
nth(6, 'šiesty').
nth(7, 'siedmy').
nth(8, 'ôsmy').
nth(9, 'deviaty').
nth(10, 'desiaty').
nth(11, 'jedenásty').
nth(12, 'dvanásty').
nth(13, 'trinásty').
nth(14, 'štrnásty').
nth(15, 'pätnásty').
nth(16, 'šestnásty').
nth(17, 'sedemnásty').


%%% distance measure
distance(Dist) == T :- Dist < 1000, dist(Dist, F), append(F, 'metrov',T).
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


distance(Dist) == ['viac ako jeden kilometer'] :- Dist < 1500.
distance(Dist) == ['viac ako 2 kilometre'] :- Dist < 3000.
distance(Dist) == ['viac ako 3 kilometre'] :- Dist < 4000.
distance(Dist) == ['viac ako 4 kilometre'] :- Dist < 5000.
distance(Dist) == ['viac ako 5 kilometrov'] :- Dist < 6000.
distance(Dist) == ['viac ako 6 kilometrov'] :- Dist < 7000.
distance(Dist) == ['viac ako 7 kilometrov'] :- Dist < 8000.
distance(Dist) == ['viac ako 8 kilometrov'] :- Dist < 9000.
distance(Dist) == ['viac ako 9 kilometrov'] :- Dist < 10000.
distance(Dist) == ['viac ako', X, 'kilometrov'] :- D is Dist/1000, dist(D, X).



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
