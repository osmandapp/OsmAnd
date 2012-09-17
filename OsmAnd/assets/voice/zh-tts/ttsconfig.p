:- op('==', xfy, 500).
version(101).
language(zh).

% before each announcement (beep)
preamble - [].


%% TURNS 
turn('left', ['左轉 ']).
turn('left_sh', ['向左急轉 ']).
turn('left_sl', ['稍向左轉 ']).
turn('right', ['右轉 ']).
turn('right_sh', ['向右急轉 ']).
turn('right_sl', ['稍向右轉 ']).
turn('right_keep', ['靠右 ']).
turn('left_keep', ['靠左 ']).

prepare_turn(Turn, Dist) == ['請準備 ', D, ' 後 ', M] :- distance(Dist) == D, turn(Turn, M).
turn(Turn, Dist) == [D, ' 後 ',M] :- distance(Dist) == D, turn(Turn, M).
turn(Turn) == M :- turn(Turn, M).

prepare_make_ut(Dist) == ['請準備', D, ' 後迴轉 '] :- distance(Dist) == D.
make_ut(Dist) == [D, ' 後請迴轉'] :- distance(Dist) == D.
make_ut == ['請迴轉 '].
make_ut_wp == ['可能的話, 請迴轉 '].

prepare_roundabout(Dist) == ['請準備', D,' 後進入圓環 '] :- distance(Dist) == D.
roundabout(Dist, _Angle, Exit) == [D, ' 後進入圓環, 然後在 ', E, ' 出口離開'] :- distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['在 ', E, ' 出口離開'] :- nth(Exit, E).

go_ahead == ['直直往前開 '].
go_ahead(Dist) == ['沿著馬路往前 ', D]:- distance(Dist) == D.

and_arrive_destination == ['然後可達終點 '].
and_arrive_intermediate == ['and arrive at your via point '].
reached_intermediate == ['you have reached your via point'].

then == ['然後 '].
reached_destination == ['抵達終點 '].
bear_right == ['靠右 '].
bear_left == ['靠左 '].

route_new_calc(Dist) == ['路程有 ', D, ' 遠'] :- distance(Dist) == D.
route_recalc(Dist) == ['重新計算路程, 距離有 ', D] :- distance(Dist) == D.

location_lost == ['接收不到 g p s 信號 '].


%% 
nth(1, '第一個 ').
nth(2, '第二個 ').
nth(3, '第三個 ').
nth(4, '第四個 ').
nth(5, '第五個 ').
nth(6, '第六個 ').
nth(7, '第七個 ').
nth(8, '第八個 ').
nth(9, '第九個 ').
nth(10, '第十個 ').
nth(11, '第十一個 ').
nth(12, '第十二個 ').
nth(13, '第十三個 ').
nth(14, '第十四個 ').
nth(15, '第十五個 ').
nth(16, '第十六個 ').
nth(17, '第十七個 ').


%%% distance measure
distance(Dist) == [ X, ' 公尺'] :- Dist < 100, D is round(Dist/10.0)*10, num_atom(D, X).
distance(Dist) == [ X, ' 公尺'] :- Dist < 1000, D is round(2*Dist/100.0)*50, num_atom(D, X).
distance(Dist) == ['約 1 公里 '] :- Dist < 1500.
distance(Dist) == ['約 ', X, ' 公里 '] :- Dist < 10000, D is round(Dist/1000.0), num_atom(D, X).
distance(Dist) == [ X, ' 公里 '] :- D is round(Dist/1000.0), num_atom(D, X).


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
