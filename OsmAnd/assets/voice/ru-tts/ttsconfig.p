:- op('==', xfy, 500).
version(101).
language(ru).

% before each announcement (beep)
preamble - [].


%% TURNS 
turn('left', ['поверните налево ']).
turn('left_sh', ['резко поверните налево ']).
turn('left_sl', ['плавно поверните налево ']).
turn('right', ['поверните направо ']).
turn('right_sh', ['резко поверните направо ']).
turn('right_sl', ['плавно поверните направо ']).
turn('right_keep', ['держитесь правее ']).
turn('left_keep', ['держитесь левее ']).

prepare_turn(Turn, Dist) == ['Приготовьтесь через ', D, ' ', M] :- distance(Dist) == D, turn(Turn, M).
turn(Turn, Dist) == ['Через ', D, M] :- distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).

prepare_make_ut(Dist) == ['Через ', D, ' выполните разворот'] :- distance(Dist) == D.
make_ut(Dist) ==  ['Через ', D, ' выполните разворот'] :- distance(Dist) == D.
make_ut == ['Выполните разворот '].
make_ut_wp == ['Выполните разворот '].

prepare_roundabout(Dist) == ['Приготовьте через ', D, ' круг'] :- distance(Dist) == D.
roundabout(Dist, _Angle, Exit) == ['Через ', D, ' круг, выполните ', E, 'съезд'] :- distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['Выполните ', E, ' съезд'] :- nth(Exit, E).

go_ahead == ['Продолжайте движение прямо '].
go_ahead(Dist) == ['Продолжайте движение ', D]:- distance(Dist) == D.

and_arrive_destination == ['и вы прибудете в пункт назначения '].
and_arrive_intermediate == ['и вы прибудете в промежуточный пункт '].
reached_intermediate == ['вы прибыли в промежуточный пункт'].
reached_destination == ['вы прибыли в пункт назначения '].

then == ['затем '].
bear_right == ['держитесь правее '].
bear_left == ['держитесь левее '].

route_new_calc(Dist) == ['Маршрут составляет ', D] :- distance(Dist) == D.
route_recalc(Dist) == ['маршрут пересчитывается, расстояние ', D] :- distance(Dist) == D.

location_lost == ['g p s потеря сигнала '].


%% 
nth(1, 'первый ').
nth(2, 'второй ').
nth(3, 'третий ').
nth(4, 'четвертый ').
nth(5, 'пятый ').
nth(6, 'шестой ').
nth(7, 'седьмой ').
nth(8, 'восьмой ').
nth(9, 'девятый ').
nth(10, 'десятый ').
nth(11, 'одиннадцатый ').
nth(12, 'двенадцатый ').
nth(13, 'тринадцатый ').
nth(14, 'четырнадцатый ').
nth(15, 'пятнадцатый ').
nth(16, 'шестнадцатый ').
nth(17, 'семнадцатый ').


%%% distance measure
distance(Dist) == [ X, ' метров'] :- Dist < 100, D is round(Dist/10.0)*10, num_atom(D, X).
distance(Dist) == [ X, ' метров'] :- Dist < 1000, D is round(2*Dist/100.0)*50, num_atom(D, X).
distance(Dist) == ['около одного километра '] :- Dist < 1500.
distance(Dist) == ['около ', X, Km] :- Dist < 10000, D is round(Dist/1000.0), num_atom(D, X), plural_km(D, Km).

plural_km(D, ' километр ') :- 1 is D mod 10.
plural_km(D, ' километра ') :- Mod is D mod 10, Mod < 5,  Mod > 1.
plural_km(_D, ' километров ').

distance(Dist) == [ X, ' километров '] :- D is round(Dist/1000.0), num_atom(D, X).

on_street == ['на', X] :- next_street(X).
off_route == ['Вы отклонились от маршрута'].
attention == ['Внимание'].
speed_alarm == ['Вы превысили допустимую скорость'].


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