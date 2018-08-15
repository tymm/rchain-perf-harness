FROM openjdk:8u171-jre-slim-stretch

COPY ./runner /runner

ENV hosts "peer0.rchain.coop peer1.rchain.coop peer2.rchain.coop peer3.rchain.coop peer4.rchain.coop" 
ENV path "/contracts"
ENV sessions 1 
ENV loops 1 
ENV ratio 1

WORKDIR /runner

CMD ./continuous.sh "$hosts" $path $sessions $ratio $loops