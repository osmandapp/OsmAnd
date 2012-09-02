% Czech navigation commands optimized for SVOX Classic TTS engine

% TODO: 
% maybe use "zahněte" instead of "odbočte"
% optimize "pak držte se vlevo"  -> "pak se držte vlevo"

:- op('==', xfy, 500).
version(101).
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
turn('right_keep', ['držte se vpravo']).
turn('left_keep', ['držte se vlevo']).

pturn('left', ['vlevo']).
pturn('left_sh', ['ostře vlevo']).
pturn('left_sl', ['mírně vlevo']).
pturn('right', ['vpravo']).
pturn('right_sh', ['ostře vpravo']).
pturn('right_sl', ['mírně vpravo']).

prepare_turn(Turn, Dist) == ['po', D, 'budete odbočovat', M] :- distance(Dist,locative) == D, pturn(Turn, M).
turn(Turn, Dist) == ['po', D, M] :- distance(Dist,locative) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).

prepare_make_ut(Dist) == ['po', D, 'se budete otáčet zpět'] :- distance(Dist,locative) == D.
make_ut(Dist) == ['po', D, 'se otočte zpět'] :- distance(Dist,locative) == D.
make_ut == ['otočte se zpět'].
make_ut_wp == ['vraťte se jakmile to bude možné'].


prepare_roundabout(Dist) == ['po', D, 'přijedete na kruhový objezd'] :- distance(Dist,locative) == D.
roundabout(Dist, _Angle, Exit) == ['po', D, 'vjeďte na kruhový objezd', 'a zvolte', E, 'výjezd'] :- distance(Dist,locative) == D, nth(Exit, nominative, E).
roundabout(_Angle, Exit) == ['vyjeďte', E, 'výjezdem'] :- nth(Exit, instrumental, E).

% SVOX bug workaround - see below
go_ahead == ['pokračujte rovně'].
go_ahead(Dist) == ['pokračujte', D]:- distance(Dist,workaround) == D.

and_arrive_destination == ['a dorazíte do cíle'].

then == ['pak'].
reached_destination == ['dorazili jste do cíle'].
bear_right == ['držte se vpravo'].
bear_left == ['držte se vlevo'].

route_new_calc(Dist) == ['cesta je dlouhá', D] :- distance(Dist,accusative) == D.
route_recalc(Dist) == ['přepočítávám. cesta je dlouhá', D] :- distance(Dist,accusative) == D.

location_lost == ['ztráta signálu'].


%% 
nth(1, nominative, 'první').
nth(1, instrumental, 'prvním').
nth(2, nominative, 'druhý').
nth(2, instrumental, 'druhým').
nth(3, nominative, 'třetí').
nth(3, instrumental, 'třetím').
nth(4, nominative, 'čtvrtý').
nth(4, instrumental, 'čtvrtým').
nth(5, nominative, 'pátý').
nth(5, instrumental, 'pátým').
nth(6, nominative, 'šestý').
nth(6, instrumental, 'šestým').
nth(7, nominative, 'sedmý').
nth(7, instrumental, 'sedmým').
nth(8, nominative, 'osmý').
nth(8, instrumental, 'osmým').
nth(9, nominative, 'devátý').
nth(9, instrumental, 'devátým').
nth(10, nominative, 'desátý').
nth(10, instrumental, 'desátým').
nth(11, nominative, 'jedenáctý').
nth(11, instrumental, 'jedenáctým').
nth(12, nominative, 'dvanáctý').
nth(12, instrumental, 'dvanáctým').
nth(13, nominative, 'třináctý').
nth(13, instrumental, 'třináctým').
nth(14, nominative, 'čtrnáctý').
nth(14, instrumental, 'čtrnáctým').
nth(15, nominative, 'patnáctý').
nth(15, instrumental, 'patnáctým').
nth(16, nominative, 'šestnáctý').
nth(16, instrumental, 'šestnáctým').
nth(17, nominative, 'sedmnáctý').
nth(17, instrumental, 'sedmnáctým').


%%% distance measure - accusative
% workaround of wrong declination in SVOX
% example: "pokracujte pet metru"
% SVOX bug: without "dál" says "pokračujte osmdesáti metrů" instead of "pokračujte osmdesát metrů"
% "pokračujte rovně 80 metrů" does not work either
% SVOX bug: "pokracujte dal priblizne 3 kilometry" is pronounced as "pokracujte dal priblizne tremi kilometry"
distance(Dist,workaround) == [ 'dál', X, 'metrů'] :- Dist < 100, D is round(Dist/10.0)*10, num_atom(D, X).
distance(Dist,workaround) == [ 'dál', X, 'metrů'] :- Dist < 1000, D is round(2*Dist/100.0)*50, num_atom(D, X).
distance(Dist,workaround) == ['přibližně jeden kilometr'] :- Dist < 1500.
distance(Dist,workaround) == ['přibližně', X, 'kilometry'] :- Dist < 4500, D is round(Dist/1000.0), num_atom(D, X).
distance(Dist,workaround) == ['dál přibližně', X, 'kilometrů'] :- Dist < 10000, D is round(Dist/1000.0), num_atom(D, X).
distance(Dist,workaround) == [ 'dál', X, 'kilometrů'] :- D is round(Dist/1000.0), num_atom(D, X).

%%% distance measure - accusative
% example: "pokracujte pet metru"
distance(Dist,accusative) == [ X, 'metrů'] :- Dist < 100, D is round(Dist/10.0)*10, num_atom(D, X).
distance(Dist,accusative) == [ X, 'metrů'] :- Dist < 1000, D is round(2*Dist/100.0)*50, num_atom(D, X).
distance(Dist,accusative) == ['přibližně jeden kilometr'] :- Dist < 1500.
distance(Dist,accusative) == ['přibližně', X, 'kilometry'] :- Dist < 4500, D is round(Dist/1000.0), num_atom(D, X).
distance(Dist,accusative) == ['přibližně', X, 'kilometrů'] :- Dist < 10000, D is round(Dist/1000.0), num_atom(D, X).
distance(Dist,accusative) == [ X, 'kilometrů'] :- D is round(Dist/1000.0), num_atom(D, X).

%%% distance measure - locative
% example: "po peti metrech zabocte vpravo"
distance(Dist,locative) == [ X, 'metrech'] :- Dist < 100, D is round(Dist/10.0)*10, num_atom(D, X).
distance(Dist,locative) == [ X, 'metrech'] :- Dist < 1000, D is round(2*Dist/100.0)*50, num_atom(D, X).
distance(Dist,locative) == ['přibližně jednom kilometru'] :- Dist < 1500.
distance(Dist,locative) == ['přibližně', X, 'kilometrech'] :- Dist < 10000, D is round(Dist/1000.0), num_atom(D, X).
distance(Dist,locative) == [ X, 'kilometrech'] :- D is round(Dist/1000.0), num_atom(D, X).


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
