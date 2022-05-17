set -e

TMP=our-tmp-folder

[ -e "$TMP" ] && rm -r "$TMP"
mkdir "$TMP"
git archive HEAD -o "$TMP"/prisma-git.tar
cd "$TMP"

tar xf prisma-git.tar
rm prisma-git.tar

docker build -t prisma-docker .
#docker save prisma-docker -o prisma-docker.tar
docker-squash prisma-docker -f 2c8ea4a77f53 -t prisma-squashed --output-path prisma-squashed.tar
tar --exclude='./PrismaFiles/DockerEvaluations/*' \
    -czvf prisma-docker-image.tar.gz README.md LICENSE PrismaFiles prisma-squashed.tar

echo ============================================================================================
echo now testing whether the docker image works
echo ============================================================================================
docker image rm prisma-squashed
docker load -i prisma-squashed.tar
docker run -it -e HOST_UID=$(id -u) -v $PWD/PrismaFiles:/app prisma

# 70MB 40MB 300MB 689MB
# 619MB g++ make python3 npm nodejs

