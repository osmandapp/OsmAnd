:- op('==', xfy, 500).
version(100).
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

prepare_turn(Turn, Dist) == ['Приготовьтесь через ', D, ' ', M] :- 
			distance(Dist) == D, turn(Turn, M).
turn(Turn, Dist) == ['Через ', D, M] :- 
			distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).

prepare_make_ut(Dist) == ['Через ', D, ' выполните разворот'] :- 
		distance(Dist) == D.

prepare_roundabout(Dist) == ['Приготовьте через ', D, ' круг'] :- 
		distance(Dist) == D.

make_ut(Dist) ==  ['Через ', D, ' выполните разворот'] :-
			distance(Dist) == D.
make_ut == ['Выполните разворот '].

roundabout(Dist, _Angle, Exit) == ['Через ', D, ' круг, выполните ', E, 'съезд'] :- distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['Выполните ', E, ' съезд'] :- nth(Exit, E).

and_arrive_destination == ['и вы прибудете в пункт назначения ']. % Miss and?
then == ['затем '].
reached_destination == ['вы прибыли в пункт назначения '].
bear_right == ['держитесь правее '].
bear_left == ['держитесь левее '].
route_recalc(_Dist) == []. % ['recalculating route '].  %nothing to said possibly beep?	
route_new_calc(Dist) == ['Маршрут составляет ', D] :- distance(Dist) == D. % nothing to said possibly beep?

go_ahead(Dist) == ['Продолжайте движение ', D]:- distance(Dist) == D.
go_ahead == ['Продолжайте движение прямо '].

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
distance(Dist) == [ X, ' meters'] :- Dist < 100, D is round(Dist/10)*10, num_atom(D, X).
distance(Dist) == [ X, ' meters'] :- Dist < 1000, D is round(2*Dist/100)*50, num_atom(D, X).
distance(Dist) == ['более одного километра '] :- Dist < 1500.
distance(Dist) == ['около ', X, ' километов '] :- Dist < 10000, D is round(Dist/1000), num_atom(D, X).
distance(Dist) == [ X, ' километов '] :- D is round(Dist/1000), num_atom(D, X).

%% resolve command main method
%% if you are familar with Prolog you can input specific to the whole mechanism,
%% by adding exception cases.
flatten(X, Y) :- flatten(X, [], Y), !.
flatten([], Acc, Acc).
flatten([X|Y], Acc, Res):- 
		flatten(Y, Acc, R), flatten(X, R, Res).
flatten(X, Acc, [X|Acc]).

resolve(X, Y) :- resolve_impl(X,Z), flatten(Z, Y).
resolve_impl([],[]).
resolve_impl([X|Rest], List) :- resolve_impl(Rest, Tail), ((X == L) -> append(L, Tail, List); List = Tail).