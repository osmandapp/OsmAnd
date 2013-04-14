:- op('==', xfy, 500).
version(101).
language(pl).

% before each announcement (beep)
preamble - [].


%% TURNS 
turn('left', ['skręć w lewo ']).
turn('left_sh', ['skręć ostro w lewo ']).
turn('left_sl', ['skręć lekko w lewo ']).
turn('right', ['skręć w prawo ']).
turn('right_sh', ['skręć ostro w prawo ']).
turn('right_sl', ['skręć lekko w prawo ']).
turn('right_keep', ['trzymaj się prawej strony']).
turn('left_keep', ['trzymaj się lewej strony']).

prepare_turn(Turn, Dist) == ['Za ', D, M] :- distance(Dist) == D, turn(Turn, M).
turn(Turn, Dist) == ['Za ', D, M] :- distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).

prepare_make_ut(Dist) == ['Za ', D, ' zawróć'] :- distance(Dist) == D.
make_ut(Dist) ==  ['Za ', D, ' zawróć'] :- distance(Dist) == D.
make_ut == ['Zawróć '].
make_ut_wp == ['Jeśli to możliwe, zawróć '].

prepare_roundabout(Dist) == ['Za ', D, ' wjedź na rondo'] :- distance(Dist) == D.
roundabout(Dist, _Angle, Exit) == ['Za ', D, ' wjedź na rondo i wybierz ', E, 'zjazd'] :- distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == [ 'Wybierz ', E, ' zjazd'] :- nth(Exit, E).

go_ahead == ['Jedź prosto '].
go_ahead(Dist) == ['Jedź prosto przez ', D]:- distance(Dist) == D.

and_arrive_destination == [', następnie dojedź do celu '].
and_arrive_intermediate == [', następnie dojedź do punktu pośredniego '].
reached_intermediate == ['punkt pośredni został osiągnięty'].

then == [', następnie '].
reached_destination == ['cel podróży został osiągnięty '].
bear_right == ['trzymaj się prawej strony '].
bear_left == ['trzymaj się lewej strony '].

route_new_calc(Dist) == ['Długość trasy to ', D] :- distance(Dist) == D.
route_recalc(Dist) == ['Nowa trasa wyznaczona, jej długość to ', D] :- distance(Dist) == D.

location_lost == ['Utracono sygnał GPS '].


%% 
nth(1, 'pierwszy ').
nth(2, 'drugi ').
nth(3, 'trzeci ').
nth(4, 'czwarty ').
nth(5, 'piąty ').
nth(6, 'szósty ').
nth(7, 'siódmy ').  
nth(8, 'ósmy ').
nth(9, 'dziewiąty ').
nth(10, 'dziesiąty ').
nth(11, 'jedenasty ').
nth(12, 'dwunasty ').
nth(13, 'trzynasty ').
nth(14, 'czternasty ').
nth(15, 'piętnasty ').
nth(16, 'szestasty ').
nth(17, 'siedemnasty ').


%%% distance measure
distance(Dist) == [ X, ' metrów'] :- Dist < 100, D is round(Dist/10.0)*10, num_atom(D, X).
distance(Dist) == [ X, ' metrów'] :- Dist < 1000, D is round(2*Dist/100.0)*50, num_atom(D, X).
distance(Dist) == ['około jeden kilometr '] :- Dist < 1500.
distance(Dist) == ['około ', X, ' kilometry '] :- Dist < 4500, D is round(Dist/1000.0), num_atom(D, X).
distance(Dist) == ['około ', X, ' kilometrów '] :- Dist < 10000, D is round(Dist/1000.0), num_atom(D, X).
distance(Dist) == [ X, ' kilometrów '] :- D is round(Dist/1000.0), num_atom(D, X).

on_street == ['na', X] :- next_street(X).
off_route == ['Znajdujesz się poza trasą'].
attention == ['Uwaga'].
speed_alarm == ['Przekraczasz dozwoloną prędkość'].

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
