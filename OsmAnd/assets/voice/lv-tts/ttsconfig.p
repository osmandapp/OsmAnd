:- op('==', xfy, 500).
version(101).
language(lv).

% before each announcement (beep)
preamble - [].


%% TURNS 
turn('left', ['griezties pa kreisi ']).
turn('left_sh', ['strauji pagriezties pa kreisi ']).
turn('left_sl', ['pagriezties pa kreisi ']).
turn('right', ['griezties pa labi ']).
turn('right_sh', ['strauji pagriezties pa labi ']).
turn('right_sl', ['pagriezties pa labi ']).
turn('right_keep', ['turēties pa labi ']).
turn('left_keep', ['turēties pa kreisi ']).

prepare_turn(Turn, Dist) == ['Pēc ', D, ' gatavoties pa', M] :- distance(Dist) == D, turn(Turn, M).
turn(Turn, Dist) == ['Pēc ', D, M] :- distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).

prepare_make_ut(Dist) == ['Gatavojaties apgriezties pēc ', D] :- distance(Dist) == D.
make_ut(Dist) == ['Pēc ', D, ' apgriežaties '] :- distance(Dist) == D.
make_ut == ['Apgriežaties '].
make_ut_wp == ['Apgriežaties pie pirmās iespējas '].

prepare_roundabout(Dist) == ['Sagatvojaties lokveida kustībai pēc ', D] :- distance(Dist) == D.
roundabout(Dist, _Angle, Exit) == ['Pēc ', D, ' iebrauciet lokveida krustojumā, un tad brauciet ', E, 'pagriezienā'] :- distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['izbrauciet ', E, 'izbrauktuvē'] :- nth(Exit, E).

go_ahead == ['Dodaties taisni uz priekšu '].
go_ahead(Dist) == ['Brauciet pa ceļu ', D]:- distance2(Dist) == D.

and_arrive_destination == ['un ierodaties galapunktā '].
and_arrive_intermediate == ['and arrive at your via point '].
reached_intermediate == ['you have reached your via point'].

then == ['tad '].
reached_destination == ['jūs esiet nokļuvis galapunktā '].
bear_right == ['turēties pa labi '].
bear_left == ['turēties pa kreisi '].

route_new_calc(Dist) == ['Brauciens ir ', D] :- distance2(Dist) == D.
route_recalc(Dist) == ['Maršruts ir pārēķināts, attālums ir ', D] :- distance2(Dist) == D.

location_lost == ['pazudis g p s signāls '].


%% 
nth(1, 'pirmajā ').
nth(2, 'otrajā ').
nth(3, 'trešajā ').
nth(4, 'ceturtajā ').
nth(5, 'piektajā ').
nth(6, 'sestajā ').
nth(7, 'septītajā ').
nth(8, 'astotajā ').
nth(9, 'devītajā ').
nth(10, 'desmitajā ').
nth(11, 'vienpadsmitajā ').
nth(12, 'divpadsmitajā ').
nth(13, 'trīspadsmitajā ').
nth(14, 'četrpadsmitajā ').
nth(15, 'piecpadsmitajā ').
nth(16, 'sešpadsmitajā ').
nth(17, 'septiņpadsmitajā ').


%%% distance measure - Pēc xxx 
distance(Dist) == [ X, ' meteriem'] :- Dist < 100, D is round(Dist/10.0)*10, num_atom(D, X).
distance(Dist) == [ X, ' meteriem'] :- Dist < 1000, D is round(2*Dist/100.0)*50, num_atom(D, X).
distance(Dist) == ['aptuveni viena kilometera '] :- Dist < 1500.
distance(Dist) == ['aptuveni ', X, ' kilometeriem '] :- Dist < 10000, D is round(Dist/1000.0), num_atom(D, X).
distance(Dist) == [ X, ' kilometriem '] :- D is round(Dist/1000.0), num_atom(D, X).
%%% distance mesure - Attālums ir xxx ....metri...
distance2(Dist) == [ X, ' meteri'] :- Dist < 100, D is round(Dist/10.0)*10, num_atom(D, X).
distance2(Dist) == [ X, ' meteri'] :- Dist < 1000, D is round(2*Dist/100.0)*50, num_atom(D, X).
distance2(Dist) == ['aptuveni 1 kilometrs '] :- Dist < 1500.
distance2(Dist) == ['aptuveni ', X, ' kilometri '] :- Dist < 10000, D is round(Dist/1000.0), num_atom(D, X).
distance2(Dist) == [ X, ' kilometri '] :- D is round(Dist/1000.0), num_atom(D, X).


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