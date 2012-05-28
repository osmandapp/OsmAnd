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

prepare_turn(Turn, Dist) == ['Pçc ', D, ' pagriezties ', M] :- distance(Dist) == D, turn(Turn, M).
turn(Turn, Dist) == ['Pçc ', D, M] :- distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).

prepare_make_ut(Dist) == ['Gatavojaties apgriezties pçc ', D] :- distance(Dist) == D.
make_ut(Dist) == ['Pçc ', D, ' apgrieþaties '] :- distance(Dist) == D.
make_ut == ['Apgrieþaties '].
make_ut_wp == ['Apgrieþaties pie pirmâs iespçjas '].

prepare_roundabout(Dist) == ['Sagatvojaties lokveida kustîbai ', D] :- distance(Dist) == D.
roundabout(Dist, _Angle, Exit) == ['Pçc ', D, ' iebrauciet lokveida krustojumâ, un tad brauciet ', E, 'pagriezienâ'] :- distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['izbrauciet ', E, 'izbrauktuvç'] :- nth(Exit, E).

go_ahead == ['Dodaties taisni uz priekðu '].
go_ahead(Dist) == ['Brauciet pa ceïu ', D]:- distance(Dist) == D.

and_arrive_destination == ['un ierodaties galapunktâ '].

then == ['tad '].
reached_destination == ['jûs esiet nokïuvis galapunktâ '].
bear_right == ['turieties pa labi '].
bear_left == ['turieties pa kreisi '].

route_new_calc(Dist) == ['Brauciens ir ', D] :- distance(Dist) == D.
route_recalc(Dist) == ['Marðruts ir pârçíinâts, attâlums ', D] :- distance(Dist) == D.

location_lost == ['pazudis g p s signâls '].


%% 
nth(1, 'pirmais ').
nth(2, 'otrais ').
nth(3, 'treðais ').
nth(4, 'ceturtais ').
nth(5, 'piektais ').
nth(6, 'sestais ').
nth(7, 'septîtais ').
nth(8, 'astotais ').
nth(9, 'devîtais ').
nth(10, 'desmit ').
nth(11, 'vienpadsmitais ').
nth(12, 'divpadsmitais ').
nth(13, 'trîspadsmitais ').
nth(14, 'èetrpadsmitais ').
nth(15, 'piecpadsmitais ').
nth(16, 'seðpadsmitais ').
nth(17, 'septiòpadsmitais ').


%%% distance measure
distance(Dist) == [ X, ' meteriem'] :- Dist < 100, D is round(Dist/10)*10, num_atom(D, X).
distance(Dist) == [ X, ' meteriem'] :- Dist < 1000, D is round(2*Dist/100)*50, num_atom(D, X).
distance(Dist) == ['aptuveni 1 kilometers '] :- Dist < 1500.
distance(Dist) == ['aptuveni ', X, ' kilometeri '] :- Dist < 10000, D is round(Dist/1000), num_atom(D, X).
distance(Dist) == [ X, ' kilometri '] :- D is round(Dist/1000), num_atom(D, X).


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