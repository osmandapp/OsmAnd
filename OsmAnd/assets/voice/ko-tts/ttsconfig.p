:- op('==', xfy, 500).
version(101).
language(ko).

% before each announcement (beep)
preamble - [].


%% TURNS 
turn('left', ['좌회전']).
turn('left_sh', ['크게 좌회전']).
turn('left_sl', ['좌회전']).
turn('right', ['우회전']).
turn('right_sh', ['크게 우회전']).
turn('right_sl', ['우회전']).
turn('right_keep', ['오른쪽에서 계속 가세요 ']).
turn('left_keep', ['왼쪽에서 계속 가세요 ']).

prepare_turn(Turn, Dist) == [D, ' 앞에서 ', M, '을 준비하세요 '] :- 	distance(Dist) == D, turn(Turn, M).
turn(Turn, Dist) == [D, ' 앞에서 ', M, '하세요 '] :- 	distance(Dist) == D, turn(Turn, M).
turn(Turn) == [M, '하세요 '] :- turn(Turn, M).

prepare_make_ut(Dist) == [D, ' 앞에서 U턴을 준비하세요 '] :- distance(Dist) == D.
make_ut(Dist) == [D, ' 앞에서 U턴하세요 '] :- 	distance(Dist) == D.
make_ut == ['지금 U턴하세요 '].
make_ut_wp == ['가능한 경우에, U턴하세요 '].

prepare_roundabout(Dist) == [D, ' 앞에서 로타리 진입을 준비하세요 '] :- 	distance(Dist) == D.
roundabout(Dist, _Angle, Exit) == [D, ' 앞에서 로타리에 진입하시고 ', E, ' 출구로 나가세요 '] :- distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == [E, ' 출구로 나가세요 '] :- nth(Exit, E).

go_ahead == ['직진을 계속하세요 '].
go_ahead(Dist) == [D, ' 직진하세요 ']:- distance(Dist) == D.

and_arrive_destination == [' 다음은 목적지에 도착합니다 ']. % Miss and?
and_arrive_intermediate == ['and arrive at your via point '].
reached_intermediate == ['you have reached your via point'].

then == [', 다음은 '].
reached_destination == ['목적지에 도착하였습니다 '].
bear_right == ['오른쪽에서 계속 가세요 '].
bear_left == ['왼쪽에서 계속 가세요 '].

route_new_calc(Dist) == ['총 거리는 ', D, ' 입니다 '] :- distance(Dist) == D.
route_recalc(Dist) == ['경로가 재탐색되었습니다. 거리는  ', D, ' 입니다 '] :- distance(Dist) == D.


location_lost == ['g p s 신호가 없습니다 '].


%% 
nth(1, '첫번째 ').
nth(2, '두번째 ').
nth(3, '세번째 ').
nth(4, '네번째 ').
nth(5, '다섯번째 ').
nth(6, '여섯번째 ').
nth(7, '일곱번째 ').
nth(8, '여덟번째 ').
nth(9, '아홉번째 ').
nth(10, '열번째 ').
nth(11, '열한번째 ').
nth(12, '열두번째 ').
nth(13, '열세번째 ').
nth(14, '열네번째 ').
nth(15, '열다섯번째 ').
nth(16, '열여섯번째 ').
nth(17, '열일곱번째 ').


%%% distance measure
distance(Dist) == [ X, ' 미터 '] :- Dist < 100, D is round(Dist/10.0)*10, num_atom(D, X).
distance(Dist) == [ X, ' 미터 '] :- Dist < 1000, D is round(2*Dist/100.0)*50, num_atom(D, X).
distance(Dist) == ['약, 1 킬로미터 '] :- Dist < 1500.
distance(Dist) == ['약, ', X, ' 킬로미터 '] :- Dist < 10000, D is round(Dist/1000.0), num_atom(D, X).
distance(Dist) == [ X, ' 킬로미터 '] :- D is round(Dist/1000.0), num_atom(D, X).


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