:- op('==', xfy, 500).
version(101).
language(hi).

% before each announcement (beep)
preamble - [].


%% TURNS 
turn('left', [' बाये ']).
turn('left_sh', [' तेजी से बाये ']).
turn('left_sl', [' हलके से बाये ']).
turn('right', [' दायने ']).
turn('right_sh', [' तेजी से दायने ']).
turn('right_sl', [' हलके से दायने ']).
turn('right_keep', ['दायने रहे ']).
turn('left_keep', ['बाये रहे  ']).

prepare_turn(Turn, Dist) == [D, ' के बाद ', M, ' मुडने के लिये तयार रहे'] :- distance(Dist) == D, turn(Turn, M).
turn(Turn, Dist) == [D, ' के बाद ', M, ' मुडिये'] :- distance(Dist) == D, turn(Turn, M).
turn(Turn) == [M, ' मुडिये'] :- turn(Turn, M).

prepare_make_ut(Dist) == [D, 'के बाद वापस मुडने के लिये तयार रहे '] :- distance(Dist) == D.
make_ut(Dist) == [D, ' के बाद वापस मुडिये '] :- distance(Dist) == D.
make_ut == [' वापस मुडिये '].
make_ut_wp == [' जब संभव हो तब वापस मुडिये '].

prepare_roundabout(Dist) == [D, 'के बाद वापस मुडने के लिये तयार रहे '] :- distance(Dist) == D.
roundabout(Dist, _Angle, Exit) == [D, ' के बाद वापस मुडिये और बाहर जाने का ', E, 'मार्ग चुने '] :- distance(Dist) == D, nth(Exit, E).
roundabout(_Angle, Exit) == ['बाहर जाने का ', E, 'मार्ग चुने '] :- nth(Exit, E).

go_ahead == [' सीधे आगे जाये '].
go_ahead(Dist) == [D, ' तक रास्ते का पालन करे ']:- distance(Dist) == D.

and_arrive_destination == [' और अपनी मंजिल पर पहोचे '].
and_arrive_intermediate == ['and arrive at your via point '].
reached_intermediate == ['you have reached your via point'].

then == ['और फिर '].
reached_destination == ['आप अपनी मंजिल पर पहोच चुके है '].
bear_right == ['दायने रहे '].
bear_left == ['बाये रहे '].

route_new_calc(Dist) == ['अंतर ', D, ' है'] :- distance(Dist) == D.
route_recalc(Dist) == ['मार्ग पुनर्गणना, अंतर ', D] :- distance(Dist) == D.

location_lost == ['जी पी एस सिग्नल नही '].


%% 
nth(1, 'पहला ').
nth(2, 'दूसरा ').
nth(3, 'तीसरा ').
nth(4, 'चौथा ').
nth(5, 'पांचवा ').
nth(6, 'छटवा ').
nth(7, 'सातवा ').
nth(8, 'आठवा ').
nth(9, 'नववा ').
nth(10, 'दसवा ').
nth(11, 'ग्यारहवा ').
nth(12, 'बारहवा ').
nth(13, 'तेरहवा ').
nth(14, 'चौदहवा ').
nth(15, 'पंधरहवा ').
nth(16, 'सोलहवा ').
nth(17, 'सतरहवा ').


%%% distance measure
distance(Dist) == [ X, ' मीटर '] :- Dist < 100, D is round(Dist/10.0)*10, num_atom(D, X).
distance(Dist) == [ X, ' मीटर '] :- Dist < 1000, D is round(2*Dist/100.0)*50, num_atom(D, X).
distance(Dist) == ['साधारण 1 किलोमीटर '] :- Dist < 1500.
distance(Dist) == ['साधारण ', X, ' किलोमीटर '] :- Dist < 10000, D is round(Dist/1000.0), num_atom(D, X).
distance(Dist) == [ X, ' किलोमीटर '] :- D is round(Dist/1000.0), num_atom(D, X).


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