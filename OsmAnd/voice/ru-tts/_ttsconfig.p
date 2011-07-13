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
distance(Dist) == T :- Dist < 1000, dist(Dist, F), append(F, ' meters',T).
dist(D, ['10 ']) :-  D < 15, !.
dist(D, ['20 ']) :-  D < 25, !.
dist(D, ['30 ']) :-  D < 35, !.
dist(D, ['40 ']) :-  D < 45, !.
dist(D, ['50 ']) :-  D < 55, !.
dist(D, ['60 ']) :-  D < 65, !.
dist(D, ['70 ']) :-  D < 75, !.
dist(D, ['80 ']) :-  D < 85, !.
dist(D, ['90 ']) :-  D < 95, !.
dist(D, ['100 ']) :-  D < 125, !.
dist(D, ['150 ']) :-  D < 175, !.
dist(D, ['200 ']) :-  D < 225, !.
dist(D, ['250 ']) :-  D < 275, !.
dist(D, ['300 ']) :-  D < 325, !.
dist(D, ['350 ']) :-  D < 375, !.
dist(D, ['400 ']) :-  D < 425, !.
dist(D, ['450 ']) :-  D < 475, !.
dist(D, ['500 ']) :-  D < 525, !.
dist(D, ['550 ']) :-  D < 575, !.
dist(D, ['600 ']) :-  D < 625, !.
dist(D, ['650 ']) :-  D < 675, !.
dist(D, ['700 ']) :-  D < 725, !.
dist(D, ['750 ']) :-  D < 775, !.
dist(D, ['800 ']) :-  D < 825, !.
dist(D, ['850 ']) :-  D < 875, !.
dist(D, ['900 ']) :-  D < 925, !.
dist(D, ['950 ']) :-  D < 975, !.
dist(D, ['1000 ']) :-  !.

distance(Dist) == ['более одного километра '] :- Dist < 1500.
distance(Dist) == ['около двух километров '] :- Dist < 2500.
distance(Dist) == ['около трех километров '] :- Dist < 3500.
distance(Dist) == ['около четырех километров '] :- Dist < 4500.
distance(Dist) == ['около пяти километров '] :- Dist < 5500.
distance(Dist) == ['около шести километров '] :- Dist < 6500.
distance(Dist) == ['около семи километров '] :- Dist < 7500.
distance(Dist) == ['около восьми километров '] :- Dist < 8500.
distance(Dist) == ['около девяти километров '] :- Dist < 9500.
distance(Dist) == ['около ', X, ' километов '] :- D is Dist/1000, dist(D, X).

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