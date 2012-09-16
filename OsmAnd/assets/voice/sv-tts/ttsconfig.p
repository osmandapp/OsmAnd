:- op('==', xfy, 500).
version(101).
language(sv).

% before each announcement (beep)
preamble - [].


%% TURNS 
turn('left', ['sväng vänster ']).
turn('left_sh', ['sväng skarpt vänster ']).
turn('left_sl', ['sväng svagt vänster ']).
turn('right', ['sväng höger ']).
turn('right_sh', ['sväng skarpt höger ']).
turn('right_sl', ['sväng lätt höger ']).
turn('right_keep', ['håll åt höger ']).
turn('left_keep', ['håll åt vänster ']).

prepare_turn(Turn, Dist) == [D, ' kvar, sedan ', M] :- distance(Dist) == D, turn(Turn, M).
turn(Turn, Dist) == ['Om ', D, ', ', M] :- distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).

prepare_make_ut(Dist) == ['Förbered för en u-sväng om ', D] :- distance(Dist) == D.
make_ut(Dist) == ['Om ', D, ' gör en u-sväng '] :- distance(Dist) == D.
make_ut == ['Gör en u-sväng '].
make_ut_wp == ['Gör en u-sväng så snart som möjligt '].

prepare_roundabout(Dist) == ['Det kommer en rondell om ', D] :- distance(Dist) == D.
roundabout(Dist, _Angle, Exit) == ['Om ', D, ' kör in i rondellen och ta ', E, 'utfarten'] :- distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['ta den ', E, 'utfarten'] :- nth(Exit, E).

go_ahead == ['Kör rakt fram '].
go_ahead(Dist) == ['Följ den här vägen ', D]:- distance(Dist) == D.

and_arrive_destination == ['och du är framme '].
and_arrive_intermediate == ['and arrive at your via point '].
reached_intermediate == ['you have reached your via point'].

then == ['sedan '].
reached_destination == ['du är framme '].
bear_right == ['håll åt höger '].
bear_left == ['håll åt vänster '].

route_new_calc(Dist) == ['Resan är ', D] :- distance(Dist) == D.
route_recalc(Dist) == ['Ny väg beräknad, resan är ', D] :- distance(Dist) == D.

location_lost == ['GPS-signalen borttappad '].


%% 
nth(1, 'första ').
nth(2, 'andra ').
nth(3, 'tredje ').
nth(4, 'fjärde ').
nth(5, 'femte ').
nth(6, 'sjätte ').
nth(7, 'sjunde ').
nth(8, 'åttonde ').
nth(9, 'nionde ').
nth(10, 'tionde ').
nth(11, 'elfte ').
nth(12, 'tolfte ').
nth(13, 'trettonde ').
nth(14, 'fjortonde ').
nth(15, 'femtonde ').
nth(16, 'sextonde ').
nth(17, 'sjuttonde ').


%%% distance measure
distance(Dist) == [ X, ' meter'] :- Dist < 100, D is round(Dist/10.0)*10, num_atom(D, X).
distance(Dist) == [ X, ' meter'] :- Dist < 1000, D is round(2*Dist/100.0)*50, num_atom(D, X).
distance(Dist) == ['ungefär en kilometer '] :- Dist < 1500.
distance(Dist) == ['ungefär ', X, ' kilometer '] :- Dist < 10000, D is round(Dist/1000.0), num_atom(D, X).
distance(Dist) == [ X, ' kilometer '] :- D is round(Dist/1000.0), num_atom(D, X).


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
