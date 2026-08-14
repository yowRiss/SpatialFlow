Name:           spatialflow
Version:        %{!?version:1.8.0}%{?version:}
Release:        %{!?release:1}%{?release:}
Summary:        SpatialFlow desktop music player

License:        GPL-3.0-or-later
URL:            https://github.com/yowRiss/SpatialFlow
Source0:        spatialflow-app.tar.gz
BuildArch:      x86_64

# The desktop player uses vlcj, which discovers Fedora's libVLC runtime through
# the standard linker path. Declaring this keeps an RPM installation from
# producing a visually working app whose playback engine cannot start.
Requires:       vlc-libs

%description
SpatialFlow is a Compose Desktop music player with local-library playback,
YouTube Music streaming, synchronized lyrics, playlists, and audio effects.

%prep
%setup -q -c -T
tar -xzf %{SOURCE0}

%install
rm -rf %{buildroot}
install -d %{buildroot}%{_libexecdir}/spatialflow
cp -a spatialflow/. %{buildroot}%{_libexecdir}/spatialflow/
install -d %{buildroot}%{_bindir}
ln -s %{_libexecdir}/spatialflow/bin/spatialflow %{buildroot}%{_bindir}/spatialflow

%files
%{_bindir}/spatialflow
%{_libexecdir}/spatialflow

%changelog
* Thu Aug 14 2026 SpatialFlow maintainers <maintainers@spatialflow.app> - %{version}-%{release}
- Package the Compose Desktop app image and require the system libVLC runtime.
