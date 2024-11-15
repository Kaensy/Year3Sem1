cmake_minimum_required(VERSION 3.28)
project(MPI_Var2)

# Specify the C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# For Windows with MS-MPI
if(WIN32)
    # Try to find MS-MPI in the default installation path
    set(MSMPI_SDK "C:/Program Files (x86)/Microsoft SDKs/MPI")
    set(MSMPI_BIN "C:/Program Files/Microsoft MPI")

    # Set MPI include and library paths
    set(MPI_C_INCLUDE_PATH "${MSMPI_SDK}/Include")
    set(MPI_CXX_INCLUDE_PATH "${MSMPI_SDK}/Include")

    if(CMAKE_SIZEOF_VOID_P EQUAL 8)
        # 64-bit
        set(MPI_C_LIBRARIES "${MSMPI_SDK}/Lib/x64/msmpi.lib")
        set(MPI_CXX_LIBRARIES "${MSMPI_SDK}/Lib/x64/msmpi.lib")
    else()
        # 32-bit
        set(MPI_C_LIBRARIES "${MSMPI_SDK}/Lib/x86/msmpi.lib")
        set(MPI_CXX_LIBRARIES "${MSMPI_SDK}/Lib/x86/msmpi.lib")
    endif()

    # Add include directories for MS-MPI
    include_directories(${MPI_C_INCLUDE_PATH})
endif()

# Find MPI package
find_package(MPI)

if(NOT MPI_FOUND)
    message(STATUS "Manual MS-MPI configuration...")
    set(MPI_FOUND TRUE)
    set(MPI_CXX_FOUND TRUE)
endif()

# Add the executable
add_executable(MPI_Var2 main.cpp)

# Link MPI libraries
if(WIN32)
    target_link_libraries(MPI_Var2 PRIVATE ${MPI_CXX_LIBRARIES})
else()
    target_link_libraries(MPI_Var2 PRIVATE MPI::MPI_CXX)
endif()

# Optional: Set additional compiler warnings
if(MSVC)
    target_compile_options(MPI_Var2 PRIVATE /W4)
else()
    target_compile_options(MPI_Var2 PRIVATE -Wall -Wextra)
endif()