:- op('==', xfy, 500).
version(101).
language(nl).

% before each announcement (beep)
preamble - [].


%% TURNS 
turn('left', ['links afslaan ']).
turn('left_sh', ['scherpe bocht naar links ']).
turn('left_sl', ['links afbuigen  ']).
turn('right', ['rechts afslaan ']).
turn('right_sh', ['scherpe bocht naar rechts ']).
turn('right_sl', ['rechts afbuigen ']).
turn('right_keep', ['rechts aanhouden ']).
turn('left_keep', ['links aanhouden ']).

prepare_turn(Turn, Dist) == ['Houdt rekening met ', M, ' na ', D] :- distance(Dist) == D, turn(Turn, M).
turn(Turn, Dist) == ['Na ', D, M] :- distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).

prepare_make_ut(Dist) == ['Graag omkeren na ', D ] :- distance(Dist) == D. 
make_ut(Dist) == ['Na ', D, ' omkeren'] :- distance(Dist) == D.
make_ut == ['Graag nu omkeren '].
make_ut_wp == ['Indien mogelijk, graag nu omkeren '].

prepare_roundabout(Dist) == ['Houdt rekening met een rotonde na ', D] :- distance(Dist) == D.
roundabout(Dist, _Angle, Exit) == ['Na ', D, ' de rotonde oprijden en neem dan de ', E, 'afslag'] :- distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['Neem de ', E, 'afslag'] :- nth(Exit, E).

go_ahead == ['Deze weg blijven volgen '].
go_ahead(Dist) == ['De weg ', D,' volgen']:- distance(Dist) == D.

and_arrive_destination == ['dan hebt u uw bestemming bereikt '].

and_arrive_intermediate == ['en dan hebt u uw routepunt bereikt '].
reached_intermediate == ['u hebt uw routepunt bereikt'].

then == ['dan '].
reached_destination == ['U hebt uw Bestemming bereikt '].
bear_right == ['rechts aanhouden '].
bear_left == ['links aanhouden '].

route_new_calc(Dist) == ['De berekende afstand is ', D, ' lang'] :- distance(Dist) == D.
route_recalc(Dist) == ['Afstand is opnieuw berekend, de nieuwe afstand is', D] :- distance(Dist) == D.

location_lost == ['G P S  Signaal verloren '].


%% 
nth(1, 'eerste ').
nth(2, 'tweede ').
nth(3, 'derde ').
nth(4, 'vierde ').
nth(5, 'vijfde ').
nth(6, 'zesde ').
nth(7, 'zevende ').
nth(8, 'achtste ').
nth(9, 'negende ').
nth(10, 'tiende ').
nth(11, 'elfde ').
nth(12, 'twaalfde ').
nth(13, 'dertiende ').
nth(14, 'viertiende ').
nth(15, 'vijftiende ').
nth(16, 'zestiende ').
nth(17, 'zeventiende ').


%%% distance measure
distance(Dist) == [ X, ' meter'] :- Dist < 100, D is round(Dist/10.0)*10, num_atom(D, X).
distance(Dist) == [ X, ' meter'] :- Dist < 1000, D is round(2*Dist/100.0)*50, num_atom(D, X).
distance(Dist) == ['ongeveer een kilometer '] :- Dist < 1500.
distance(Dist) == ['ongeveer ', X, ' Kilometer '] :- Dist < 10000, D is round(Dist/1000.0), num_atom(D, X).
distance(Dist) == [ X, ' Kilometer '] :- D is round(Dist/1000.0), num_atom(D, X).


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
