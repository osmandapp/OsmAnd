:- op('==', xfy, 500).
version(100).
language(hu).

% before each announcement (beep)
preamble - [].


%% TURNS 
turn('left', ['fordulj balra ']).
turn('left_sh', ['fordulj élesen balra ']).
turn('left_sl', ['fordulj enyhén balra ']).
turn('right', ['fordulj jobbra ']).
turn('right_sh', ['fordulj élesen jobbra ']).
turn('right_sl', ['fordulj enyhén jobbra ']).

prepare_turn(Turn, Dist) == [D, ' múlva ', M] :- 
			distance(Dist) == D, turn(Turn, M).
turn(Turn, Dist) == [D, 'múlva ', M] :- 
			distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).


prepare_make_ut(Dist) == [D, ' múlva készüj fel a visszafordulásra'] :- 
		distance(Dist) == D.

prepare_roundabout(Dist) == [D, ' múlva hajts be a köforgalomba'] :- 
		distance(Dist) == D.

make_ut(Dist) == [D, ' múlva fordulj vissza '] :- 
			distance(Dist) == D.
make_ut == ['Fordulj vissza '].

roundabout(Dist, _Angle, Exit) == [D, ' múlva a körforgalomban ', E, 'kijáraton hajts ki'] :- distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['hajts ki ', E, 'kijáraton'] :- nth(Exit, E).

and_arrive_destination == ['és megérkezel az uticélhoz ']. % Miss and?
then == ['majd '].
reached_destination == ['megérkeztél az uticélhoz '].
bear_right == ['tarts jobbra '].
bear_left == ['tarts balra '].
route_recalc(_Dist) == []. % ['útvonal újratervezése '].  %nothing to said possibly beep?	
route_new_calc(Dist) == ['Az útvonal ', D] :- distance(Dist) == D. % nothing to said possibly beep?

go_ahead(Dist) == ['Menj tovább ', D, 't '] :- distance(Dist) == D.
go_ahead == ['Haladj tovább egyenesen '].

%% 
nth(1, 'az első ').
nth(2, 'a második ').
nth(3, 'a harmadik ').
nth(4, 'a negyedik ').
nth(5, 'az ötödik ').
nth(6, 'a hatodik ').
nth(7, 'a hetedik ').
nth(8, 'a nyolcadik ').
nth(9, 'a kilencedik ').
nth(10, 'a tizedik ').
nth(11, 'a tizenegyedik ').
nth(12, 'a tizenkettedik ').
nth(13, 'a tizenharmadik ').
nth(14, 'a tizennegyedik ').
nth(15, 'a tizenötödik ').
nth(16, 'a tizenhatodik ').
nth(17, 'a tizenhetedik ').


%%% distance measure
distance(Dist) == T :- Dist < 1000, dist(Dist, F), append(F, ' méter',T).
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

distance(Dist) == ['több mint 1 kilométer'] :- Dist < 1500.
distance(Dist) == ['több mint 2 kilométer'] :- Dist < 3000.
distance(Dist) == ['több mint 3 kilométer'] :- Dist < 4000.
distance(Dist) == ['több mint 4 kilométer'] :- Dist < 5000.
distance(Dist) == ['több mint 5 kilométer'] :- Dist < 6000.
distance(Dist) == ['több mint 6 kilométer'] :- Dist < 7000.
distance(Dist) == ['több mint 7 kilométer'] :- Dist < 8000.
distance(Dist) == ['több mint 8 kilométer'] :- Dist < 9000.
distance(Dist) == ['több mint 9 kilométer'] :- Dist < 10000.
distance(Dist) == ['több mint ', X, ' kilométer'] :- D is Dist/1000, dist(D, X).

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
