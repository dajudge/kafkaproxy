FROM mcr.microsoft.com/dotnet/sdk:3.1 as base

FROM base as build
ADD *.cs /src/
ADD *.csproj /src/
WORKDIR /src
RUN dotnet restore
RUN dotnet publish -c Release -o out

FROM base
COPY --from=build /src/out /app
ADD run.sh /app/
WORKDIR /app
CMD ["sleep", "600"]