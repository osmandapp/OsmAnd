:- op('==', xfy, 500).
version(101).
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
turn('right_keep', ['tarts jobbra ']).
turn('left_keep', ['tarts balra ']).

prepare_turn(Turn, Dist) == [D, ' múlva ', M] :- distance(Dist, nom) == D, turn(Turn, M).
turn(Turn, Dist) == [D, 'múlva ', M] :- distance(Dist, nom) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).

prepare_make_ut(Dist) == [D, ' múlva készülj fel a visszafordulásra'] :- distance(Dist, nom) == D.
make_ut(Dist) == [D, ' múlva fordulj vissza '] :- distance(Dist, nom) == D.
make_ut == ['Fordulj vissza '].
make_ut_wp == ['Fordulj vissza '].

prepare_roundabout(Dist) == [D, ' múlva hajts be a körforgalomba'] :- distance(Dist, nom) == D.
roundabout(Dist, _Angle, Exit) == [D, ' múlva a körforgalomból ', E, 'kijáraton hajts ki'] :- distance(Dist, nom) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['hajts ki ', E, 'kijáraton'] :- nth(Exit, E).

go_ahead == ['Haladj tovább egyenesen '].
go_ahead(Dist) == ['Menj tovább ', D] :- distance(Dist, acc) == D.

and_arrive_destination == ['és megérkezel az uticélhoz.'].
and_arrive_intermediate == ['és megérkezel a köztes célponthoz.'].
reached_intermediate == ['megérkeztél a köztes célponthoz.'].

then == ['majd '].
reached_destination == ['megérkeztél az uticélhoz.'].
bear_right == ['tarts jobbra '].
bear_left == ['tarts balra '].

route_new_calc(Dist) == ['Az útvonal ', D] :- distance(Dist, nom) == D.
route_recalc(Dist) == ['útvonal újratervezése, az útvonal ', D] :- distance(Dist, nom) == D.

location_lost == ['nem található dzsípíesz pozíció '].


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
%%% nom=nominative, acc=accusative
distance(Dist, nom) == [ X, ' méter'] :- Dist < 100, D is round(Dist/10.0)*10, num_atom(D, X).
distance(Dist, acc) == [ X, ' métert'] :- Dist < 100, D is round(Dist/10.0)*10, num_atom(D, X).
distance(Dist, nom) == [ X, ' méter'] :- Dist < 1000, D is round(2*Dist/100.0)*50, num_atom(D, X).
distance(Dist, acc) == [ X, ' métert'] :- Dist < 1000, D is round(2*Dist/100.0)*50, num_atom(D, X).
distance(Dist, nom) == ['körülbelül 1 kilométer'] :- Dist < 1500.
distance(Dist, acc) == ['körülbelül 1 kilométert'] :- Dist < 1500.
distance(Dist, nom) == ['mintegy ', X, ' kilométer'] :- Dist < 10000, D is round(Dist/1000.0), num_atom(D, X).
distance(Dist, acc) == ['mintegy ', X, ' kilométert'] :- Dist < 10000, D is round(Dist/1000.0), num_atom(D, X).
distance(Dist, nom) == [ X, ' kilométer'] :- D is round(Dist/1000.0), num_atom(D, X).
distance(Dist, acc) == [ X, ' kilométert'] :- D is round(Dist/1000.0), num_atom(D, X).


%% resolve command main method
%% if you are familar with Prolog you can input specific to the whole mechanism,
%% by adding exception cases.
flatten(X, Y) :- flatten(X, [], Y), !.
flatten([], Acc, Acc).
flatten([X|Y], Acc, Res):- flatten(Y, Acc, R), flatten(X, R, Res).
flatten(X, Acc, [X|Acc]).

resolve(X, Y) :- resolve_impl(X,Z), flatten(Z, Y).
resolve_impl([],[]).
resolve_impl([X|Rest], List) :- resolve_impl(Rest, Tail), ((X == L) -> append(L, Tail, List); List = Tail).
