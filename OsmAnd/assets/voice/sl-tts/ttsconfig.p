:- op('==', xfy, 500).
version(101).
language(sl).

% before each announcement (beep)
preamble - [].


%% TURNS 
turn('left', ['zavij levo ']).
turn('left_sh', ['zavij ostro levo ']).
turn('left_sl', ['zavij rahlo levo']).
turn('right', ['zavij desno ']).
turn('right_sh', ['zavij ostro desno ']).
turn('right_sl', ['zavij rahlo desno ']).
turn('right_keep', ['drži se desno ']).
turn('left_keep', ['drži se levo ']).

prepare_turn(Turn, Dist) == ['Čez ', D, M] :- distance(Dist) == D, turn(Turn, M).
turn(Turn, Dist) == ['Čez ', D, M] :- distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).

prepare_make_ut(Dist) == ['Pripravi se na obrat nazaj čez ', D] :- distance(Dist) == D.
make_ut(Dist) == ['Čez ', D, ' obrni nazaj '] :- distance(Dist) == D.
make_ut == ['Obrni nazaj '].
make_ut_wp == ['Čim bo mogoče obrni nazaj '].


prepare_roundabout(Dist) == ['Pripravite se na krožišče čez ', D] :- distance(Dist) == D.
roundabout(Dist, _Angle, Exit) == ['Čez ', D, ' zapeljite v krožišče, nato pa uporabite ', E, ' izvoz '] :- distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['Uporabite ', E, ' izvoz '] :- nth(Exit, E).

go_ahead == ['Pojdi naravnost naprej '].
go_ahead(Dist) == ['Nadaljuj po cesti še ', D]:- distance(Dist) == D.

and_arrive_destination == ['in prispete na cilj '].

then == ['nato '].
reached_destination == ['prispeli ste na cilj '].
and_arrive_intermediate == ['in prispete na vmesni cilj '].
reached_intermediate == ['Prispeli ste na vmesni cilj'].
bear_right == ['drži se desno '].
bear_left == ['drži se levo '].

route_new_calc(Dist) == ['Pot bo dolga ', D] :- distance(Dist) == D.
route_recalc(Dist) == ['Izračunana je nova pot dolžine ', D] :- distance(Dist) == D.

location_lost == ['Ni več G P S  signala '].


%% 
nth(1, 'prvi ').
nth(2, 'drugi ').
nth(3, 'tretji ').
nth(4, 'četrti ').
nth(5, 'peti ').
nth(6, 'šesti ').
nth(7, 'sedmi ').
nth(8, 'osmi ').
nth(9, 'deveti ').
nth(10, 'deseti ').
nth(11, 'enajsti ').
nth(12, 'dvanajsti ').
nth(13, 'trinajsti ').
nth(14, 'štirinajsti ').
nth(15, 'petnajsti ').
nth(16, 'šestnajsti ').
nth(17, 'sedemnajsti ').


%%% distance measure
distance(Dist) == [ X, ' metrov']               :- Dist < 100,   D is round(Dist/10.0)*10,           num_atom(D, X).
distance(Dist) == [ X, ' metrov']               :- Dist < 1000,  D is round(2*Dist/100.0)*50,        num_atom(D, X).
distance(Dist) == ['približno 1 kilometer ']        :- Dist < 1500.
distance(Dist) == ['približno 2 kilometra ']        :- Dist < 2500.
distance(Dist) == ['približno ', X, ' kilometre '] :- Dist < 4500, D is round(Dist/1000.0),            num_atom(D, X).
distance(Dist) == ['približno ', X, ' kilometrov '] :- Dist < 10000, D is round(Dist/1000.0),            num_atom(D, X).
distance(Dist) == [ X, ' kilometrov ']          :-               D is round(Dist/1000.0),            num_atom(D, X).
%% TODO: general slovenian 4 plural forms: 101&1001 kilometer, 102&1002 kilometra, 103&104 kilometre...


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