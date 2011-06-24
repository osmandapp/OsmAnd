:- op('==', xfy, 500).
version(100).
language(ru).

% before each announcement (beep)
preamble - [].

%% TURNS 
turn('left', ['поверните налево ']).
turn('left_sh', ['резко поверните налево ']).
turn('left_sl', ['слегка поверните налево ']).
turn('right', ['поверните направо ']).
turn('right_sh', ['резко поверните направо ']).
turn('right_sl', ['слегка поверните направо ']).

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
dist(D, ['10 ']) :-  D < 20, !.
dist(D, ['20 ']) :-  D < 30, !.
dist(D, ['30 ']) :-  D < 40, !.
dist(D, ['40 ']) :-  D < 50, !.
dist(D, ['50 ']) :-  D < 60, !.
dist(D, ['60 ']) :-  D < 70, !.
dist(D, ['70 ']) :-  D < 80, !.
dist(D, ['80 ']) :-  D < 90, !.
dist(D, ['90 ']) :-  D < 100, !.
dist(D, ['100 ']) :-  D < 150, !.
dist(D, ['150 ']) :-  D < 200, !.
dist(D, ['200 ']) :-  D < 250, !.
dist(D, ['250 ']) :-  D < 300, !.
dist(D, ['300 ']) :-  D < 350, !.
dist(D, ['350 ']) :-  D < 400, !.
dist(D, ['400 ']) :-  D < 450, !.
dist(D, ['450 ']) :-  D < 500, !.
dist(D, ['500 ']) :-  D < 550, !.
dist(D, ['550 ']) :-  D < 600, !.
dist(D, ['600 ']) :-  D < 650, !.
dist(D, ['650 ']) :-  D < 700, !.
dist(D, ['700 ']) :-  D < 750, !.
dist(D, ['750 ']) :-  D < 800, !.
dist(D, ['800 ']) :-  D < 850, !.
dist(D, ['850 ']) :-  D < 900, !.
dist(D, ['900 ']) :-  D < 950, !.
dist(D, ['950 ']) :-  !.

distance(Dist) == ['более одного километра '] :- Dist < 1500.
distance(Dist) == ['более двух километров '] :- Dist < 3000.
distance(Dist) == ['более трех километров '] :- Dist < 4000.
distance(Dist) == ['более четырех километров '] :- Dist < 5000.
distance(Dist) == ['более пяти километров '] :- Dist < 6000.
distance(Dist) == ['более шести километров '] :- Dist < 7000.
distance(Dist) == ['более семи километров '] :- Dist < 8000.
distance(Dist) == ['более восьми километров '] :- Dist < 9000.
distance(Dist) == ['более девяти километров '] :- Dist < 10000.
distance(Dist) == ['более чем ', X, ' километов '] :- D is Dist/1000, dist(D, X).

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