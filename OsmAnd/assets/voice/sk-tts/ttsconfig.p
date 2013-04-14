:- op('==', xfy, 500).
version(101).
language(sk).

% before each announcement (beep)
preamble - [].


%% TURNS 
turn('left', ['zahnite doľava']).
turn('left_sh', ['zahnite prudko doľava']).
turn('left_sl', ['zahnite mierne doľava']).
turn('right', ['zahnite doprava']).
turn('right_sh', ['zahnite prudko doprava']).
turn('right_sl', ['zahnite mierne doprava']).
turn('right_keep', ['držte sa vpravo']).
turn('left_keep', ['držte sa vľavo']).

pturn('left', ['doľava']).
pturn('left_sh', ['prudko doľava']).
pturn('left_sl', ['mierne doľava']).
pturn('right', ['doprava']).
pturn('right_sh', ['prudko doprava']).
pturn('right_sl', ['mierne doprava']).

prepare_turn(Turn, Dist) == ['o ', D, 'buďte pripravený zahnúť ', M] :- distance(Dist) == D, pturn(Turn, M).
turn(Turn, Dist) == ['o ', D, M] :- distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).

prepare_make_ut(Dist) == ['o ', D, 'buďte pripravený otočiť sa naspäť'] :- distance(Dist) == D.
make_ut(Dist) == ['o', D, 'sa otočte naspäť'] :- distance(Dist) == D.
make_ut == ['otočte sa naspäť'].
make_ut_wp == ['keď to bude možné, otočte sa naspäť'].

prepare_roundabout(Dist) == ['o ', D, 'buďte pripravený vojsť do kruhového objazdu'] :- distance(Dist) == D.
roundabout(Dist, _Angle, Exit) == ['o ', D, ' vojdite do kruhového objazdu ', 'a opustite ho cez ', E, 'výjazd'] :- distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['choďte cez ', E, 'výjazd'] :- nth(Exit, E).

go_ahead == ['pokračujte rovno'].
go_ahead(Dist) == ['pokračujte ', D]:- distance(Dist) == D.

and_arrive_destination == ['a dorazíte do cieľa'].
and_arrive_intermediate == ['a dorazíte cez Váš prechodný bod '].
reached_intermediate == ['dorazili ste k Vášmu prechodnému bodu'].

then == ['potom '].
reached_destination == ['dorazili ste do cieľa'].
bear_right == ['držte sa vpravo'].
bear_left == ['držte sa vľavo'].

route_new_calc(Dist) == ['Cesta je dlhá ', D] :- distance(Dist) == D.	
route_recalc(Dist) == ['Cesta prepočítaná, vzdialenosť ', D] :- distance(Dist) == D.

location_lost == ['strata satelytného signálu '].

on_street == ['do ulice ', X] :- next_street(X).
off_route == ['odchýlili ste sa od trasy'].
attention == ['pozor'].
speed_alarm == ['prekročili ste maximálnu povolenú rýchlosť'].

%% 
nth(1, 'prvý ').
nth(2, 'druhý ').
nth(3, 'tretí ').
nth(4, 'štvrtý ').
nth(5, 'piaty ').
nth(6, 'šiesty ').
nth(7, 'siedmy ').
nth(8, 'ôsmy ').
nth(9, 'deviaty ').
nth(10, 'desiaty ').
nth(11, 'jedenásty ').
nth(12, 'dvanásty ').
nth(13, 'trinásty ').
nth(14, 'štrnásty ').
nth(15, 'pätnásty ').
nth(16, 'šestnásty ').
nth(17, 'sedemnásty ').


%%% distance measure
distance(Dist) == [ X, ' metrov'] :- Dist < 100, D is round(Dist/10.0)*10, num_atom(D, X).
distance(Dist) == [ X, ' metrov'] :- Dist < 1000, D is round(2*Dist/100.0)*50, num_atom(D, X).
distance(Dist) == ['približne jeden kilometer '] :- Dist < 1500.
distance(Dist) == ['približne ', X, ' kilometre'] :- Dist < 4500, D is round(Dist/1000.0), num_atom(D, X).
distance(Dist) == ['približne ', X, ' kilometrov '] :- Dist < 10000, D is round(Dist/1000.0), num_atom(D, X).
distance(Dist) == [ X, ' kilometrov '] :- D is round(Dist/1000.0), num_atom(D, X).


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
