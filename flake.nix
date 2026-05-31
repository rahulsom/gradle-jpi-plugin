{
  description = "Development environment for gradle-jpi-plugin";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  };

  outputs = { nixpkgs, ... }:
    let
      systems = [
        "x86_64-linux"
        "aarch64-linux"
        "x86_64-darwin"
        "aarch64-darwin"
      ];

      forAllSystems = nixpkgs.lib.genAttrs systems;
    in
    {
      devShells = forAllSystems (system:
        let
          pkgs = nixpkgs.legacyPackages.${system};
          jdk = pkgs.jdk17;
        in
        {
          default = pkgs.mkShell {
            packages = [
              jdk
              pkgs.git
              pkgs.openssh
            ];

            JAVA_HOME = jdk.home;

            shellHook = ''
              export PATH="$JAVA_HOME/bin:$PATH"

              if [ -z "''${SSH_AUTH_SOCK:-}" ]; then
                for socket in \
                  "/run/user/$(id -u)/gcr/ssh" \
                  "/run/user/$(id -u)/keyring/ssh" \
                  "/run/user/$(id -u)/ssh-agent"; do
                  if [ -S "$socket" ]; then
                    export SSH_AUTH_SOCK="$socket"
                    break
                  fi
                done
              fi

              echo "Using Java: $(java -version 2>&1 | head -n 1)"
            '';
          };
        });
    };
}
